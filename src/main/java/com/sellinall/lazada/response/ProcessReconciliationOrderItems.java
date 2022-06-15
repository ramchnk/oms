package com.sellinall.lazada.response;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.enums.SIAOrderSettlementStatus;

public class ProcessReconciliationOrderItems implements Processor {
	static Logger log = Logger.getLogger(ProcessReconciliationOrderItems.class.getName());

	public void process(Exchange exchange) throws Exception {
		List<ArrayList<String>> orderList = exchange.getProperty("orderList", ArrayList.class);
		Map<String, BasicDBObject> orderMap = new HashMap<String, BasicDBObject>();
		for (int index = 1; index < orderList.size(); index++) {
			HashMap<String, Object> firstRowMap = getMap(orderList.get(0), orderList.get(index));
			groupItems(exchange, firstRowMap, orderMap);
		}
		List<BasicDBObject> processedOrderList = new ArrayList<BasicDBObject>();
		for (Map.Entry<String, BasicDBObject> entry : orderMap.entrySet()) {	
			processedOrderList.add(entry.getValue());
		}
		exchange.getOut().setBody(processedOrderList);
	}

	public void groupItems(Exchange exchange, HashMap<String, Object> firstRowMap, Map<String, BasicDBObject> orderMap)
			throws Exception {
		String orderNumber = firstRowMap.get("Order No.").toString();
		if (orderMap.containsKey(orderNumber)) {
			processUnSequenceOrder(exchange, orderMap.get(orderNumber), firstRowMap);
			return;
		}
		BasicDBObject order = new BasicDBObject();
		order.put("orderNumber", orderNumber);
		order.put("timeSettled", getSettlementDate(firstRowMap.get("timeSettled").toString()));
		order.put("transactionPeriod", firstRowMap.get("Statement").toString());
		Map<String, BasicDBObject> orderItems = new HashMap<String, BasicDBObject>();
		String orderItemNumber = firstRowMap.get("Order Item No.").toString();
		if (orderItemNumber.trim().isEmpty()) {
			orderItemNumber = firstRowMap.get("Reference").toString();
		}
		orderItems.put(orderItemNumber, new BasicDBObject());
		processOrderItems(exchange, firstRowMap, orderItems.get(orderItemNumber));
		BasicDBList orderItemMap = getOrderItemsFromMap(orderItems);
		order.put("orderItems", orderItemMap);
		orderMap.put(orderNumber, order);
		int noOfOrders = exchange.getProperty("noOfOrders", Integer.class);
		exchange.setProperty("noOfOrders", noOfOrders++);
	}

	private void processUnSequenceOrder(Exchange exchange, BasicDBObject order, HashMap<String, Object> sheetRow) {
		BasicDBList orderItems = (BasicDBList) order.get("orderItems");
		String orderItemNumber = sheetRow.get("Order Item No.").toString();
		if (orderItemNumber.trim().isEmpty()) {
			orderItemNumber = sheetRow.get("Reference").toString();
		}
		BasicDBObject orderItem = getOrderItemFromList(orderItems, orderItemNumber);
		if (orderItem != null) {
			processOrderItems(exchange, sheetRow, orderItem);
		} else {
			orderItem = new BasicDBObject();
			processOrderItems(exchange, sheetRow, orderItem);
			orderItems.add(orderItem);
		}
		order.put("orderItems", orderItems);
	}

	private BasicDBObject getOrderItemFromList(BasicDBList list, String orderItemNumber) {
		for (int i = 0; i < list.size(); i++) {
			BasicDBObject orderItem = (BasicDBObject) list.get(i);
			if (orderItem.getString("orderItemID").equalsIgnoreCase(orderItemNumber)) {
				return orderItem;
			}
		}
		return null;
	}

	private void processOrderItems(Exchange exchange, HashMap<String, Object> order, BasicDBObject orderItem) {
		String currencyCode = exchange.getProperty("currencyCode", String.class);
		String orderItemID = order.get("Order Item No.").toString();
		if (orderItemID.isEmpty()) {
			orderItemID = order.get("Reference").toString();
		}
		orderItem.put("orderItemID", orderItemID);
		orderItem.put("customSKU", order.get("Seller SKU").toString());
		String paymentStatus = order.get("Paid Status").toString();
		String transactionType = order.get("Fee Name").toString();
		boolean settlementAmtMatchedBankTransfer = exchange.getProperty("settlementAmtMatchedBankTransfer",
				boolean.class);
		orderItem = getSettlementObject(transactionType, orderItem, paymentStatus, settlementAmtMatchedBankTransfer);
		Set<String> fieldsToUpdate = new HashSet<String>();
		//Now we handle only return shipping
		if (orderItem.get("feesFieldsToUpdate") != null) {
			fieldsToUpdate = (Set<String>) orderItem.get("feesFieldsToUpdate");
		}
		transactionType = transactionType.replace("Reversal ", "");
		if (transactionType.equalsIgnoreCase("Payment Fee") || transactionType.equalsIgnoreCase("Payment Fee Credit")) {
			orderItem.put("paymentFeeAmount", getSIAAmountObject(order.get("Amount").toString(), currencyCode));
			orderItem.put("paymentFeeVATAmount",
					getSIAAmountObject(order.get("VAT in Amount").toString(), currencyCode));
			fieldsToUpdate.add("paymentFeeAmount");
		} else if (transactionType.equalsIgnoreCase("Payment fee - correction for undercharge")) {
			orderItem.put("paymentFeeCorrectionDebit",
					getSIAAmountObject(order.get("Amount").toString(), currencyCode));
			orderItem.put("paymentFeeCorrectionDebitVAT",
					getSIAAmountObject(order.get("VAT in Amount").toString(), currencyCode));
			fieldsToUpdate.add("paymentFeeCorrectionDebit");
		} else if (transactionType.equalsIgnoreCase("Commission fee - correction for undercharge")) {
			orderItem.put("commissionFeeCorrectionDebit",
					getSIAAmountObject(order.get("Amount").toString(), currencyCode));
			orderItem.put("commissionFeeCorrectionDebitVAT",
					getSIAAmountObject(order.get("VAT in Amount").toString(), currencyCode));
			fieldsToUpdate.add("commissionFeeCorrectionDebit");
		} else if (transactionType.equalsIgnoreCase("Promotional Charges Vouchers")
				|| transactionType.equalsIgnoreCase("Promotional Charges Flexi-Combo")
				|| transactionType.equalsIgnoreCase("Promotional Charges Bundles")
				|| transactionType.equalsIgnoreCase("Lazcoin discount")) {
			String promotionalChargesAmountValue = (String) order.get("Amount");
			if (!promotionalChargesAmountValue.isEmpty()) {
				if (orderItem.containsField("promotionalFeeAmount")) {
					BasicDBObject promotionalFeeAmount = ((BasicDBObject) orderItem.get("promotionalFeeAmount"));
					long promotionalFeeAmountValue = promotionalFeeAmount.getLong("amount");
					long siaFormatPromotionalValue = Math.abs(CurrencyUtil
							.convertAmountToSIAFormat(Double.parseDouble(promotionalChargesAmountValue)));
					promotionalFeeAmountValue = promotionalFeeAmountValue + siaFormatPromotionalValue;
					promotionalFeeAmount.put("amount", Math.abs(promotionalFeeAmountValue));
					orderItem.put("promotionalFeeAmount", promotionalFeeAmount);
				} else {
					orderItem.put("promotionalFeeAmount",
							getSIAAmountObject(promotionalChargesAmountValue, currencyCode));
				}
				fieldsToUpdate.add("sellerDiscountAmount");
			}
		} else if (transactionType.equalsIgnoreCase("Commission")) {
			orderItem.put("commissionAmount", getSIAAmountObject(order.get("Amount").toString(), currencyCode));
			orderItem.put("commissionVATAmount",
					getSIAAmountObject(order.get("VAT in Amount").toString(), currencyCode));
			fieldsToUpdate.add("commissionAmount");
		} else if (transactionType.toLowerCase().contains("Item Price".toLowerCase())
				|| transactionType.toLowerCase().contains("Lost Claim".toLowerCase())
				|| transactionType.toLowerCase().contains("Damaged Claim".toLowerCase())
				|| transactionType.toLowerCase().contains("Wrong Status Claims".toLowerCase())) {
			orderItem.put("buyerPaidAmount", getSIAAmountObject(order.get("Amount").toString(), currencyCode));
			fieldsToUpdate.add("itemAmount");
		} else if (transactionType.equalsIgnoreCase("Shipping Fee (Paid By Customer)")) {
			long shippingAmount = CurrencyUtil
					.convertAmountToSIAFormat(Math.abs(Double.parseDouble(order.get("Amount").toString())));
			orderItem.put("shippingAmountPaid", shippingAmount);
			fieldsToUpdate.add("shippingAmount");
		} else if (transactionType.equalsIgnoreCase("Shipping Fee (Charged by Lazada)")
				|| transactionType.equalsIgnoreCase("Shipping Fee Paid by Seller")) {
			long shippingFeePaidToChannel = CurrencyUtil
					.convertAmountToSIAFormat(Math.abs(Double.parseDouble(order.get("Amount").toString())));
			long shippingFeePaidToChannelVAT = CurrencyUtil
					.convertAmountToSIAFormat(Math.abs(Double.parseDouble(order.get("VAT in Amount").toString())));
			if (orderItem.containsField("shippingFeePaidToChannel")) {
				shippingFeePaidToChannel += ((BasicDBObject) orderItem.get("shippingFeePaidToChannel"))
						.getLong("amount");
			}
			if (orderItem.containsField("shippingFeePaidToChannelVAT")) {
				shippingFeePaidToChannelVAT += ((BasicDBObject) orderItem.get("shippingFeePaidToChannelVAT"))
						.getLong("amount");
			}
			orderItem.put("shippingFeePaidToChannel",
					CurrencyUtil.getAmountObject(shippingFeePaidToChannel, currencyCode));
			if (shippingFeePaidToChannelVAT > 0) {
				orderItem.put("shippingFeePaidToChannelVAT",
						CurrencyUtil.getAmountObject(shippingFeePaidToChannelVAT, currencyCode));
			}
			fieldsToUpdate.add("shippingFeePaidToChannel");
		} else if (transactionType.equalsIgnoreCase("Other Fee")) {
			long otherFee = exchange.getProperty("totalOtherFee", Long.class);
			long SIAAmount = CurrencyUtil
					.convertAmountToSIAFormat(Math.abs(Double.parseDouble(order.get("Amount").toString())));
			exchange.setProperty("totalOtherFee", otherFee + SIAAmount);
			int noOfRowsInOtherFee = exchange.getProperty("noOfRowsInOtherFee", Integer.class);
			exchange.setProperty("noOfRowsInOtherFee", noOfRowsInOtherFee + 1);
		} else if (transactionType.equalsIgnoreCase("Seller Credit Item")) {
			long totalSellerCreditAmount = exchange.getProperty("totalSellerCreditAmount", Long.class);
			long SIAAmount = CurrencyUtil
					.convertAmountToSIAFormat(Math.abs(Double.parseDouble(order.get("Amount").toString())));
			exchange.setProperty("totalSellerCreditAmount", totalSellerCreditAmount + SIAAmount);
			int noOfRowsInSellerCredit = exchange.getProperty("noOfRowsInSellerCredit", Integer.class);
			exchange.setProperty("noOfRowsInSellerCredit", noOfRowsInSellerCredit + 1);
		} else if (transactionType.equalsIgnoreCase("Auto. Shipping fee subsidy (by Lazada)")
				|| transactionType.equalsIgnoreCase("Shipping Fee Voucher (by Lazada)")
				|| transactionType.equalsIgnoreCase("automated shipping subsidy")) {
			long shippingFeeRebateFromChannel = CurrencyUtil
					.convertAmountToSIAFormat(Math.abs(Double.parseDouble(order.get("Amount").toString())));
			if (orderItem.containsField("shippingFeeRebateFromChannel")) {
				shippingFeeRebateFromChannel += ((BasicDBObject) orderItem.get("shippingFeeRebateFromChannel"))
						.getLong("amount");
			}
			orderItem.put("shippingFeeRebateFromChannel",
					CurrencyUtil.getAmountObject(shippingFeeRebateFromChannel, currencyCode));
			fieldsToUpdate.add("shippingFeeRebateFromChannel");
		} else if (transactionType.equalsIgnoreCase("Return shipping fees")) {
			fieldsToUpdate.add("returnShippingFeePaidToChannel");
			long shippingAmount = CurrencyUtil
					.convertAmountToSIAFormat(Math.abs(Double.parseDouble(order.get("Amount").toString())));
			orderItem.put("returnShippingFeePaidToChannel", CurrencyUtil.getAmountObject(shippingAmount, currencyCode));
			long shippingAmountVAT = CurrencyUtil
					.convertAmountToSIAFormat(Math.abs(Double.parseDouble(order.get("VAT in Amount").toString())));
			if (shippingAmountVAT > 0) {
				orderItem.put("returnShippingFeePaidToChannelVAT",
						CurrencyUtil.getAmountObject(shippingAmountVAT, currencyCode));
			}
		} else if (transactionType.equalsIgnoreCase("Sponsored Affiliates")) {
			fieldsToUpdate.add("sponsoredAffiliatesFee");
			orderItem.put("sponsoredAffiliatesFee", getSIAAmountObject(order.get("Amount").toString(), currencyCode));
			orderItem.put("sponsoredAffiliatesFeeVAT",
					getSIAAmountObject(order.get("VAT in Amount").toString(), currencyCode));
		} else if (transactionType.equalsIgnoreCase("Sponsored Affiliates Refund")) {
			fieldsToUpdate.add("refundSponsoredAffiliatesFee");
			orderItem.put("refundSponsoredAffiliatesFee", getSIAAmountObject(order.get("Amount").toString(), currencyCode));
			orderItem.put("refundSponsoredAffiliatesFeeVAT",
					getSIAAmountObject(order.get("VAT in Amount").toString(), currencyCode));
		} else if (transactionType.equalsIgnoreCase("Lazada Bonus")) {
			String bonusValue = (String) order.get("Amount");
			if (!bonusValue.isEmpty()) {
				orderItem.put("lazadaBonus", getSIAAmountObject(bonusValue, currencyCode));
				fieldsToUpdate.add("sellerBorneChannelDiscount");
			}
		} else if (transactionType.equalsIgnoreCase("Lazada Bonus - LZD co-fund")
				|| transactionType.equalsIgnoreCase("Lazada Bonus - LZD co-fund Extra ")) {
			String bonusValue = (String) order.get("Amount");
			if (!bonusValue.isEmpty()) {
				if (orderItem.containsField("sellerCoFundLazadaBonus")) {
					BasicDBObject sellerCoFundLazadaBonus = ((BasicDBObject) orderItem.get("sellerCoFundLazadaBonus"));
					long bonusAmount = sellerCoFundLazadaBonus.getLong("amount");
					long siaFormatBonusAmount = Math
							.abs(CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(bonusValue)));
					bonusAmount = bonusAmount + siaFormatBonusAmount;
					sellerCoFundLazadaBonus.put("amount", Math.abs(bonusAmount));
					orderItem.put("sellerCoFundLazadaBonus", sellerCoFundLazadaBonus);
				} else {
					orderItem.put("sellerCoFundLazadaBonus", getSIAAmountObject(bonusValue, currencyCode));
				}
				fieldsToUpdate.add("sellerBorneChannelDiscount");
			}
		} else if (transactionType.equalsIgnoreCase("Lazada Bonus - Reversal")) {
			String bonusValue = (String) order.get("Amount");
			if (!bonusValue.isEmpty()) {
				orderItem.put("refundLazadaBonus", getSIAAmountObject(bonusValue, currencyCode));
				fieldsToUpdate.add("refundSellerBorneChannelDiscount");
			}
		} else if (transactionType.equalsIgnoreCase("Lazada Bonus - LZD co-fund - Reversal")) {
			String bonusValue = (String) order.get("Amount");
			if (!bonusValue.isEmpty()) {
				if (orderItem.containsField("refundSellerCoFundLazadaBonus")) {
					BasicDBObject refundSellerCoFundLazadaBonus = ((BasicDBObject) orderItem
							.get("refundSellerCoFundLazadaBonus"));
					long bonusAmount = refundSellerCoFundLazadaBonus.getLong("amount");
					long siaFormatRefundBonusAmount = Math
							.abs(CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(bonusValue)));
					bonusAmount = bonusAmount + siaFormatRefundBonusAmount;
					refundSellerCoFundLazadaBonus.put("amount", Math.abs(bonusAmount));
					orderItem.put("refundSellerCoFundLazadaBonus", refundSellerCoFundLazadaBonus);
				} else {
					orderItem.put("refundSellerCoFundLazadaBonus", getSIAAmountObject(bonusValue, currencyCode));
				}
				fieldsToUpdate.add("refundSellerCoFundLazadaBonus");
			}
		} else {
			log.warn("Unhandled Transaction Type: " + transactionType);
		}
		orderItem.put("feesFieldsToUpdate", fieldsToUpdate);
	}

	private HashMap<String, Object> getMap(ArrayList<String> header, ArrayList<String> row) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("timeSettled", row.get(0));
		for (int i = 1; i < header.size(); i++) {
			map.put(header.get(i), row.get(i));
		}
		return map;
	}

	private BasicDBList getOrderItemsFromMap(Map<String, BasicDBObject> map) {
		BasicDBList orderItems = new BasicDBList();
		for (Map.Entry<String, BasicDBObject> entry : map.entrySet()) {
			orderItems.add(entry.getValue());
		}
		return orderItems;
	}

	private Object getSIAAmountObject(String amount, String currencyCode) {
		long SIAAmount = CurrencyUtil.convertAmountToSIAFormat(Math.abs(Double.parseDouble(amount)));
		return CurrencyUtil.getAmountObject(SIAAmount, currencyCode);
	}

	private String getSettlementStatus(String paymentStatus, boolean settlementAmtMatchedBankTransfer) {
		if (settlementAmtMatchedBankTransfer) {
			return SIAOrderSettlementStatus.SETTLED.toString();
		}
		if (paymentStatus.equalsIgnoreCase("Paid")) {
			return SIAOrderSettlementStatus.SETTLED.toString();
		}
		return SIAOrderSettlementStatus.NOT_SETTLED.toString();
	}

	private String getReturnSettlementStatus(String paymentStatus, boolean settlementAmtMatchedBankTransfer) {
		if (settlementAmtMatchedBankTransfer) {
			return SIAOrderSettlementStatus.RETURN_SETTLED.toString();
		}
		if (paymentStatus.equalsIgnoreCase("Paid")) {
			return SIAOrderSettlementStatus.RETURN_SETTLED.toString();
		}
		return SIAOrderSettlementStatus.RETURN_NOT_SETTLED.toString();
	}

	private long getSettlementDate(String date) throws ParseException {
		DateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
		Date result = new Date();
		result = df.parse(date);
		return result.getTime() / 1000L;
	}

	private BasicDBObject getSettlementObject(String transactionType, BasicDBObject orderItem, String paymentStatus,
			boolean settlementAmtMatchedBankTransfer) {
		if (transactionType.toLowerCase().contains("Reversal".toLowerCase())
				|| transactionType.toLowerCase().equalsIgnoreCase("return shipping fees")
				|| transactionType.equalsIgnoreCase("Sponsored Affiliates Refund")
				|| transactionType.equalsIgnoreCase("Payment Fee Credit")) {
			orderItem.put("returnSettlementStatus",
					getReturnSettlementStatus(paymentStatus, settlementAmtMatchedBankTransfer));
			if (orderItem.containsField("settlementDetails")) {
				return (BasicDBObject) ((BasicDBObject) orderItem.get("settlementDetails")).get("refunded");
			} else {
				BasicDBObject refundedObject = new BasicDBObject();
				orderItem.put("settlementDetails", new BasicDBObject("refunded", refundedObject));
				return refundedObject;
			}
		} else {
			orderItem.put("settlementStatus", getSettlementStatus(paymentStatus, settlementAmtMatchedBankTransfer));
			return orderItem;
		}
	}
}