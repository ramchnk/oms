package com.sellinall.lazada.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.enums.SIAOrderSettlementStatus;

public class ConstructPNQMessageForReconciliation implements Processor {
	static Logger log = Logger.getLogger(ConstructPNQMessageForReconciliation.class.getName());

	public void process(Exchange exchange) throws Exception {
		BasicDBObject orderFromDB = exchange.getIn().getBody(BasicDBObject.class);
		BasicDBObject orderFromSheet = exchange.getProperty("order", BasicDBObject.class);
		BasicDBList orderItemsFromDB = (BasicDBList) orderFromDB.get("orderItems");

		for (int i = 0; i < orderItemsFromDB.size(); i++) {
			BasicDBObject orderItem = (BasicDBObject) orderItemsFromDB.get(i);
			String orderItemID = orderItem.getString("orderItemID");
			BasicDBObject orderItemFromSheet = getOrderItemFromSheet(orderItemID,
					(BasicDBList) orderFromSheet.get("orderItems"));
			if (orderItemFromSheet != null) {
				String settlementStatus;
				if (!(orderItem.containsField("settlementStatus") && orderItem.getString("settlementStatus")
						.equals(SIAOrderSettlementStatus.SETTLED.toString()))) {
					settlementStatus = getOrderSettlementStatus(orderItemFromSheet, "settlementStatus");
					orderItem.put("settlementStatus", settlementStatus);
				}
				settlementStatus = orderItem.getString("settlementStatus");
				processOrderItems(exchange, "settlementAmount", orderItemFromSheet, orderItem, settlementStatus);
				if (settlementStatus.equals(SIAOrderSettlementStatus.SETTLED.toString())) {
					orderFromDB.put("settlementStatus", settlementStatus);
				}
				if (!orderFromDB.containsField("settlementStatus")) {
					orderFromDB.put("settlementStatus", SIAOrderSettlementStatus.NOT_SETTLED.toString());
				}
				//orderItemFromSheet only contains refund object if it is present in sheet
				if (containsRefundObject(orderItemFromSheet)) {
					BasicDBObject refundObjectFromSheet = getRefundObject(orderItemFromSheet);
					BasicDBObject refundedObject;
					if (containsRefundObject(orderItem)) {
						refundedObject = getRefundObject(orderItem);
					} else {
						refundedObject = new BasicDBObject();
					}
					String returnSettlementStatus;
					if (!(orderItem.containsField("returnSettlementStatus")
							&& orderItem.getString("returnSettlementStatus")
									.equals(SIAOrderSettlementStatus.RETURN_SETTLED.toString()))) {
						returnSettlementStatus = getOrderSettlementStatus(orderItemFromSheet, "returnSettlementStatus");
						orderItem.put("returnSettlementStatus", returnSettlementStatus);
					}
					returnSettlementStatus = orderItem.getString("returnSettlementStatus");
					processOrderItems(exchange, "refundSettlementAmount", refundObjectFromSheet, refundedObject,
							returnSettlementStatus);
					if (returnSettlementStatus.equals(SIAOrderSettlementStatus.RETURN_SETTLED.toString())) {
						orderFromDB.put("returnSettlementStatus", returnSettlementStatus);
					}
					putRefundObject(orderItem, refundedObject);
				}
			}
		}

		orderFromDB.put("timeSettled", orderFromSheet.get("timeSettled"));
		if (exchange.getProperties().containsKey("timeSettlementProcessed")) {
			orderFromDB.put("timeSettlementProcessed", exchange.getProperty("timeSettlementProcessed", Long.class));
		}
		orderFromDB.put("transactionPeriod", orderFromSheet.get("transactionPeriod"));
		orderFromDB.put("nickNameID", exchange.getProperty("nickNameID"));
		orderFromDB.put("site", "lazada");
		orderFromDB.put("isReconciliation", true);
		orderFromDB.remove("notificationID");
		orderFromDB.remove("updateStatus");
		log.debug(orderFromDB);
		exchange.getOut().setBody(LazadaUtil.parseToJsonObject((DBObject) orderFromDB));
	}

	private BasicDBObject getOrderItemFromSheet(String orderItemID, BasicDBList orderItemsFromSheet) {
		for (int i = 0; i < orderItemsFromSheet.size(); i++) {
			BasicDBObject orderItem = (BasicDBObject) orderItemsFromSheet.get(i);
			if (orderItemID.equals(orderItem.getString("orderItemID"))) {
				return orderItem;
			}
		}
		return null;
	}

	private long getSettlementAmount(Exchange exchange, BasicDBObject orderItem, String settlementStatus,
			long shippingAmount, String settlementType) {
		long serviceCharge = 0;
		if (orderItem.containsField("paymentFeeAmount")) {
			serviceCharge = serviceCharge - getAmountValue(orderItem, "paymentFeeAmount");
		}
		if (orderItem.containsField("commissionAmount")) {
			serviceCharge = serviceCharge - getAmountValue(orderItem, "commissionAmount");
		}
		if (orderItem.containsField("paymentFeeCorrectionDebit")) {
			BasicDBObject amount = (BasicDBObject) orderItem.get("paymentFeeCorrectionDebit");
			serviceCharge = serviceCharge - Math.abs(amount.getLong("amount"));
		}
		if (orderItem.containsField("commissionFeeCorrectionDebit")) {
			BasicDBObject amount = (BasicDBObject) orderItem.get("commissionFeeCorrectionDebit");
			serviceCharge = serviceCharge - Math.abs(amount.getLong("amount"));
		}
		long paidAmount = 0;
		if(orderItem.containsField("buyerPaidAmount")){
			BasicDBObject buyerPiadAmount = (BasicDBObject) orderItem.get("buyerPaidAmount");
			paidAmount = Math.abs(buyerPiadAmount.getLong("amount"));
		}
		if (orderItem.containsField("promotionalFeeAmount")) {
			paidAmount = paidAmount - getAmountValue(orderItem, "promotionalFeeAmount");
		}
		if (orderItem.containsField("sellerBorneChannelDiscount")) {
			paidAmount = paidAmount - getAmountValue(orderItem, "sellerBorneChannelDiscount");
		}
		if (orderItem.containsField("refundSellerBorneChannelDiscount")) {
			paidAmount = paidAmount - getAmountValue(orderItem, "refundSellerBorneChannelDiscount");
		}
		long settlementAmount = 0;

		if (serviceCharge < 0) {
			settlementAmount = paidAmount + serviceCharge;
		} else {
			settlementAmount = paidAmount - serviceCharge;
		}
		settlementAmount += shippingAmount;
		String currencyCode = exchange.getProperty("currencyCode", String.class);

		//For ID we don't get the shipping amount from MP so it's handled in shippingFeePaidToChannel
		if (exchange.getProperty("countryCode", String.class).equals("ID")) {
			if (shippingAmount  > 0) {
				orderItem.put("shippingFeePaidToChannel", CurrencyUtil.getAmountObject(shippingAmount, currencyCode));
			}
		}
		if (orderItem.containsField("shippingFeeRebateFromChannel")) {
			settlementAmount += getAmountValue(orderItem, "shippingFeeRebateFromChannel");
		}
		if (orderItem.containsField("shippingFeePaidToChannel")) {
			settlementAmount -= getAmountValue(orderItem, "shippingFeePaidToChannel");
		}
		if (orderItem.containsField("returnShippingFeePaidToChannel")) {
			settlementAmount += getAmountValue(orderItem, "returnShippingFeePaidToChannel");
		}
		if (orderItem.containsField("marketingFee")) {
			settlementAmount -= getAmountValue(orderItem, "marketingFee");
		}
		if (settlementStatus.equals(SIAOrderSettlementStatus.NOT_SETTLED.toString())
				&& exchange.getProperties().containsKey("awaitingAmount")) {
			long awaitingAmount = exchange.getProperty("awaitingAmount", Long.class);
			exchange.setProperty("awaitingAmount", awaitingAmount + settlementAmount);
		} else if (exchange.getProperties().containsKey("receivedAmount")) {
			long receivedAmount = exchange.getProperty("receivedAmount", Long.class);
			exchange.setProperty("receivedAmount", receivedAmount + settlementAmount);
		}
		return settlementAmount;
	}

	private long getAmountValue(BasicDBObject orderItem, String key) {
		return Math.abs(((BasicDBObject) orderItem.get(key)).getLong("amount"));
	}

	private boolean containsRefundObject(BasicDBObject orderItem) {
		if (orderItem.containsField("settlementDetails")) {
			BasicDBObject settlementDetails = (BasicDBObject) orderItem.get("settlementDetails");
			if (settlementDetails.containsField("refunded")) {
				return true;
			}
		}
		return false;
	}

	private BasicDBObject getRefundObject(BasicDBObject orderItem) {
			BasicDBObject settlementDetails = (BasicDBObject) orderItem.get("settlementDetails");
			return (BasicDBObject) settlementDetails.get("refunded");
	}

	private BasicDBObject putRefundObject(BasicDBObject orderItem, BasicDBObject refundedObject) {
		BasicDBObject settlementDetails;
		if (orderItem.containsField("settlementDetails")) {
			settlementDetails = (BasicDBObject) orderItem.get("settlementDetails");
		} else {
			settlementDetails = new BasicDBObject();
			orderItem.put("settlementDetails", settlementDetails);
		}
		settlementDetails.put("refunded", refundedObject);
		return (BasicDBObject) settlementDetails.get("refunded");
	}

	private void processOrderItems(Exchange exchange, String settlementType, BasicDBObject orderItemFromSheet, BasicDBObject orderItem,
			String settlementStatus) {
		long expectedMarketPlaceCommission = 0;
		long vat = 0;
		String currencyCode = exchange.getProperty("currencyCode", String.class);
		if (orderItemFromSheet.containsField("feesFieldsToUpdate")) {
			orderItem.put("feesFieldsToUpdate", orderItemFromSheet.get("feesFieldsToUpdate"));
		}
		if (orderItemFromSheet.containsField("paymentFeeAmount")) {
			orderItem.put("paymentFeeAmount", orderItemFromSheet.get("paymentFeeAmount"));
		}
		if (orderItemFromSheet.containsField("paymentFeeVATAmount")) {
			orderItem.put("paymentFeeVATAmount", orderItemFromSheet.get("paymentFeeVATAmount"));
		}
		if (orderItemFromSheet.containsField("promotionalFeeAmount")) {
			orderItem.put("promotionalFeeAmount", orderItemFromSheet.get("promotionalFeeAmount"));
		}
		if (orderItemFromSheet.containsField("lazadaBonus")
				|| orderItemFromSheet.containsField("sellerCoFundLazadaBonus")) {
			long sellerBorneChannelDiscount = 0;
			if (orderItemFromSheet.containsField("lazadaBonus")) {
				sellerBorneChannelDiscount -= ((BasicDBObject) orderItemFromSheet.get("lazadaBonus")).getLong("amount");
			}
			if (orderItemFromSheet.containsField("sellerCoFundLazadaBonus")) {
				sellerBorneChannelDiscount += ((BasicDBObject) orderItemFromSheet.get("sellerCoFundLazadaBonus"))
						.getLong("amount");
			}
			orderItem.put("sellerBorneChannelDiscount",
					CurrencyUtil.getAmountObject(Math.abs(sellerBorneChannelDiscount), currencyCode));
		}
		if (orderItemFromSheet.containsField("refundLazadaBonus")
				|| orderItemFromSheet.containsField("refundSellerCoFundLazadaBonus")) {
			long refundSellerBorneChannelDiscount = 0;
			if (orderItemFromSheet.containsField("refundLazadaBonus")) {
				refundSellerBorneChannelDiscount -= ((BasicDBObject) orderItemFromSheet.get("refundLazadaBonus"))
						.getLong("amount");
			}
			if (orderItemFromSheet.containsField("refundSellerCoFundLazadaBonus")) {
				refundSellerBorneChannelDiscount += ((BasicDBObject) orderItemFromSheet.get("refundSellerCoFundLazadaBonus"))
						.getLong("amount");
			}
			orderItem.put("refundSellerBorneChannelDiscount",
					CurrencyUtil.getAmountObject(Math.abs(refundSellerBorneChannelDiscount), currencyCode));
		}
		if (orderItemFromSheet.containsField("commissionAmount")) {
			orderItem.put("commissionAmount", orderItemFromSheet.get("commissionAmount"));
		}
		if (orderItemFromSheet.containsField("commissionVATAmount")) {
			orderItem.put("commissionVATAmount", orderItemFromSheet.get("commissionVATAmount"));
		}
		if (orderItemFromSheet.containsField("paymentFeeCorrectionDebit")) {
			orderItem.put("paymentFeeCorrectionDebit", orderItemFromSheet.get("paymentFeeCorrectionDebit"));
		}
		if (orderItemFromSheet.containsField("paymentFeeCorrectionDebitVAT")) {
			orderItem.put("paymentFeeCorrectionDebitVAT", orderItemFromSheet.get("paymentFeeCorrectionDebitVAT"));
		}
		if (orderItemFromSheet.containsField("commissionFeeCorrectionDebitVAT")) {
			orderItem.put("commissionFeeCorrectionDebitVAT", orderItemFromSheet.get("commissionFeeCorrectionDebitVAT"));
		}
		if (orderItemFromSheet.containsField("commissionFeeCorrectionDebit")) {
			orderItem.put("commissionFeeCorrectionDebit", orderItemFromSheet.get("commissionFeeCorrectionDebit"));
		}
		if (orderItemFromSheet.containsField("buyerPaidAmount")) {
			orderItem.put("buyerPaidAmount", orderItemFromSheet.get("buyerPaidAmount"));
		}
		if (orderItemFromSheet.containsField("shippingFeePaidToChannel")) {
			orderItem.put("shippingFeePaidToChannel", orderItemFromSheet.get("shippingFeePaidToChannel"));
		}
		if (orderItemFromSheet.containsField("shippingFeePaidToChannelVAT")) {
			orderItem.put("shippingFeePaidToChannelVAT", orderItemFromSheet.get("shippingFeePaidToChannelVAT"));
		}
		if (orderItemFromSheet.containsField("sponsoredAffiliatesFee")) {
			orderItem.put("sponsoredAffiliatesFee", orderItemFromSheet.get("sponsoredAffiliatesFee"));
		}
		if (orderItemFromSheet.containsField("sponsoredAffiliatesFeeVAT")) {
			orderItem.put("sponsoredAffiliatesFeeVAT", orderItemFromSheet.get("sponsoredAffiliatesFeeVAT"));
		}
		if (orderItemFromSheet.containsField("refundSponsoredAffiliatesFee")) {
			orderItem.put("refundSponsoredAffiliatesFee", orderItemFromSheet.get("refundSponsoredAffiliatesFee"));
		}
		if (orderItemFromSheet.containsField("refundSponsoredAffiliatesFeeVAT")) {
			orderItem.put("refundSponsoredAffiliatesFeeVAT", orderItemFromSheet.get("refundSponsoredAffiliatesFeeVAT"));
		}
		if(orderItem.containsField("paymentFeeAmount")) {
			long paymentFeeAmount = ((BasicDBObject) orderItem.get("paymentFeeAmount")).getLong("amount");
			expectedMarketPlaceCommission += paymentFeeAmount;
		}
		if(orderItem.containsField("commissionAmount")) {
			long commissionAmount = ((BasicDBObject) orderItem.get("commissionAmount")).getLong("amount");
			expectedMarketPlaceCommission += commissionAmount;
		}
		long marketPlaceCommissionVAT = 0;
		if(orderItem.containsField("paymentFeeVATAmount")) {
			long paymentFeeVATAmount = ((BasicDBObject) orderItem.get("paymentFeeVATAmount")).getLong("amount");
			vat += paymentFeeVATAmount;
			marketPlaceCommissionVAT += paymentFeeVATAmount;
		}
		if(orderItem.containsField("commissionVATAmount")) {
			long commissionVATAmount = ((BasicDBObject) orderItem.get("commissionVATAmount")).getLong("amount");
			vat += commissionVATAmount;
			marketPlaceCommissionVAT += commissionVATAmount;
		}
		if(orderItem.containsField("paymentFeeCorrectionDebit")) {
			long paymentFeeCorrectionDebit = ((BasicDBObject) orderItem.get("paymentFeeCorrectionDebit")).getLong("amount");
			expectedMarketPlaceCommission += paymentFeeCorrectionDebit;
		}
		if(orderItem.containsField("commissionFeeCorrectionDebit")) {
			long commissionFeeCorrectionDebit = ((BasicDBObject) orderItem.get("commissionFeeCorrectionDebit")).getLong("amount");
			expectedMarketPlaceCommission += commissionFeeCorrectionDebit;
		}
		//marketing Fee
		long marketingFee = 0;
		if(orderItem.containsField("sponsoredAffiliatesFee")) {
			long sponsoredAffiliatesFee = ((BasicDBObject) orderItem.get("sponsoredAffiliatesFee")).getLong("amount");
			marketingFee += sponsoredAffiliatesFee;
		}
		if(orderItem.containsField("refundSponsoredAffiliatesFee")) {
			long refundSponsoredAffiliatesFee = ((BasicDBObject) orderItem.get("refundSponsoredAffiliatesFee")).getLong("amount");
			marketingFee += refundSponsoredAffiliatesFee;
		}
		if(orderItem.containsField("paymentFeeCorrectionDebitVAT")) {
			long paymentFeeCorrectionDebitVAT = ((BasicDBObject) orderItem.get("paymentFeeCorrectionDebitVAT")).getLong("amount");
			vat += paymentFeeCorrectionDebitVAT;
			marketPlaceCommissionVAT += paymentFeeCorrectionDebitVAT;
		}
		if(orderItem.containsField("commissionFeeCorrectionDebitVAT")) {
			long commissionFeeCorrectionDebitVAT = ((BasicDBObject) orderItem.get("commissionFeeCorrectionDebitVAT")).getLong("amount");
			vat += commissionFeeCorrectionDebitVAT;
			marketPlaceCommissionVAT += commissionFeeCorrectionDebitVAT;
		}
		if (orderItem.containsField("sponsoredAffiliatesFeeVAT")) {
			vat += ((BasicDBObject) orderItem.get("sponsoredAffiliatesFeeVAT")).getLong("amount");
		}
		if (orderItem.containsField("refundSponsoredAffiliatesFeeVAT")) {
			vat += ((BasicDBObject) orderItem.get("refundSponsoredAffiliatesFeeVAT")).getLong("amount");
		}
		if (orderItem.containsField("returnShippingFeePaidToChannelVAT")) {
			vat += ((BasicDBObject) orderItem.get("returnShippingFeePaidToChannelVAT")).getLong("amount");
		}
		if (orderItem.containsField("shippingFeePaidToChannelVAT")) {
			vat += ((BasicDBObject) orderItem.get("shippingFeePaidToChannelVAT")).getLong("amount");
		}
		orderItem.put("expectedMarketPlaceCommission",
				CurrencyUtil.getAmountObject(expectedMarketPlaceCommission, currencyCode));
		orderItem.put("VAT", CurrencyUtil.getAmountObject(vat, currencyCode));
		orderItem.put("marketPlaceCommissionVAT", CurrencyUtil.getAmountObject(marketPlaceCommissionVAT, currencyCode));
		orderItem.put("marketingFee", CurrencyUtil.getAmountObject(marketingFee, currencyCode));

		long shippingAmount = 0;
		if (orderItemFromSheet.containsField("shippingAmountPaid")) {
			shippingAmount = orderItemFromSheet.getLong("shippingAmountPaid");
		} else if (orderItem.containsField("shippingAmount")) {
			shippingAmount = ((BasicDBObject) orderItem.get("shippingAmount")).getLong("amount");
		}
		if (orderItemFromSheet.containsField("shippingFeeRebateFromChannel")) {
			orderItem.put("shippingFeeRebateFromChannel", orderItemFromSheet.get("shippingFeeRebateFromChannel"));
		}
		if (orderItemFromSheet.containsField("returnShippingFeePaidToChannel")) {
			orderItem.put("returnShippingFeePaidToChannel", orderItemFromSheet.get("returnShippingFeePaidToChannel"));
		}
		if (orderItemFromSheet.containsField("returnShippingFeePaidToChannelVAT")) {
			orderItem.put("returnShippingFeePaidToChannelVAT", orderItemFromSheet.get("returnShippingFeePaidToChannelVAT"));
		}
		long settlementAmount = getSettlementAmount(exchange, orderItem, settlementStatus, shippingAmount,
				settlementType);

		orderItem.put(settlementType, CurrencyUtil.getAmountObject(settlementAmount, currencyCode));
		if (orderItemFromSheet.containsField("feesFieldsToUpdate")) {
			orderItem.put("feesFieldsToUpdate", orderItemFromSheet.get("feesFieldsToUpdate"));
		}
	}

	private String getOrderSettlementStatus(BasicDBObject orderItemFromSheet, String key) {
		String status = getSettlementStatusKey(orderItemFromSheet, key);
		if (status == null) {
			if (key.equals("settlementStatus")) {
				status = SIAOrderSettlementStatus.NOT_SETTLED.toString();
			} else {
				status = SIAOrderSettlementStatus.RETURN_NOT_SETTLED.toString();
			}
		}
		return status;
	}

	private String getSettlementStatusKey(BasicDBObject orderItemFromSheet, String key) {
		if (orderItemFromSheet.containsField(key)) {
			return orderItemFromSheet.getString(key);
		}
		return null;
	}
}
