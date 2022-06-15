package com.sellinall.lazada.init;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.enums.SIAOrderSettlementStatus;

public class InitProcessReconcileData implements Processor {
	static Logger log = Logger.getLogger(InitProcessReconcileData.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONArray orderList = exchange.getProperty("orderList", JSONArray.class);
		String currencyCode = exchange.getProperty("currencyCode", String.class);
		int orderIndex = exchange.getProperty("orderIndex", Integer.class);
		JSONObject firstOrder = orderList.getJSONObject(orderIndex);
		String orderNumber = "";
		if (firstOrder.has("order_no")) {
			orderNumber = firstOrder.getString("order_no");
		} else {
			orderNumber = firstOrder.getString("Order No.");
		}
		boolean idOrderNumberEqual = true;
		BasicDBObject order = new BasicDBObject();
		order.put("orderNumber", orderNumber);
		String settlementStatus = "";
		if (firstOrder.has("paid_status")) {
			settlementStatus = firstOrder.getString("paid_status");
		} else {
			settlementStatus = firstOrder.getString("Paid Status");
		}
		order.put("settlementStatus", getSettlementStatus(settlementStatus));
		String transactionDate = "";
		if (firstOrder.has("transaction_date")) {
			transactionDate = firstOrder.getString("transaction_date");
		} else {
			transactionDate = firstOrder.getString("Transaction Date");
		}
		order.put("timeSettled", getSettlementDate(transactionDate));
		String statement = "";
		if (firstOrder.has("statement")) {
			statement = firstOrder.getString("statement");
		} else {
			statement = firstOrder.getString("Statement");
		}
		exchange.setProperty("transactionPeriod", statement);
		Map<String, BasicDBObject> orderItems = new HashMap<String, BasicDBObject>();
		do {
			JSONObject data = orderList.getJSONObject(orderIndex);
			if ((data.has("order_no") && data.get("order_no").toString().equals(orderNumber))
					|| (data.has("Order No.") && data.get("Order No.").toString().equals(orderNumber))) {
				String orderItemNumber = "";
				if(data.has("orderItem_no")) {
					orderItemNumber = data.get("orderItem_no").toString();
				} else {
					orderItemNumber = data.get("Order Item No.").toString();
				}
				if (orderItems.containsKey(orderItemNumber)) {
					processOrderItems(exchange, data, orderItems.get(orderItemNumber), currencyCode);
				} else {
					orderItems.put(orderItemNumber, new BasicDBObject());
					processOrderItems(exchange, data, orderItems.get(orderItemNumber), currencyCode);
				}
				orderIndex++;
			} else {
				idOrderNumberEqual = false;
			}
		} while (idOrderNumberEqual && (orderList.length() != orderIndex));
		BasicDBList orderItemMap = getOrderItemsFromMap(orderItems);
		order.put("orderItems", orderItemMap);
		exchange.setProperty("order", order);
		exchange.setProperty("orderIndex", orderIndex);
		exchange.setProperty("isLastRow", checkIsLastRow(exchange));
	}

	private void processOrderItems(Exchange exchange, JSONObject order, BasicDBObject orderItem,
			String currencyCode) {
		String accountNumber = "";
		if (exchange.getProperties().containsKey("accountNumber")) {
			accountNumber = (String) exchange.getProperty("accountNumber");
		}
		try {
			String orderItemNo = "";
			String amount = "";
			String vatAmount = "";
			if (order.has("orderItem_no")) {
				orderItemNo = order.getString("orderItem_no");
			} else {
				orderItemNo = order.getString("Order Item No.");
			}
			if (order.has("amount")) {
				amount = order.getString("amount");
			} else {
				amount = order.getString("Amount");
			}
			if (order.has("VAT_in_amount")) {
				vatAmount = order.getString("VAT_in_amount");
			} else {
				vatAmount = order.getString("VAT in Amount");
			}
			orderItem.put("orderItemID", orderItemNo);
			String transactionType = "";
			if (order.has("fee_name")) {
				transactionType = order.getString("fee_name");
			} else {
				transactionType = order.getString("Fee Name");
			}
			if (transactionType.equals("Payment Fee")) {
				orderItem.put("paymentFeeAmount", getSIAAmountObject(amount, currencyCode));
				orderItem.put("paymentFeeVATAmount", getSIAAmountObject(vatAmount, currencyCode));
			} else if (transactionType.equals("Commission")) {
				orderItem.put("commissionAmount", getSIAAmountObject(amount, currencyCode));
				orderItem.put("commisionVATAmount", getSIAAmountObject(vatAmount, currencyCode));
			} else if (transactionType.equals("Item Price Credit")) {
				orderItem.put("buyerPaidAmount", getSIAAmountObject(amount, currencyCode));
			} else if (transactionType.equals("Shipping Fee (Paid By Customer)")) {
				long shippingAmount = CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(amount));
				orderItem.put("shippingAmountPaid", shippingAmount);
			} else if (transactionType.equals("Other Fee")) {
				long otherFee = exchange.getProperty("totalOtherFee", Long.class);
				long SIAAmount = CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(amount));
				exchange.setProperty("totalOtherFee", otherFee + SIAAmount);
				int noOfRowsInOtherFee = exchange.getProperty("noOfRowsInOtherFee", Integer.class);
				exchange.setProperty("noOfRowsInOtherFee", noOfRowsInOtherFee + 1);
			} else if (transactionType.equals("Seller Credit Item")) {
				long totalSellerCreditAmount = exchange.getProperty("totalSellerCreditAmount", Long.class);
				long SIAAmount = CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(amount));
				exchange.setProperty("totalSellerCreditAmount", totalSellerCreditAmount + SIAAmount);
				int noOfRowsInSellerCredit = exchange.getProperty("noOfRowsInSellerCredit", Integer.class);
				exchange.setProperty("noOfRowsInSellerCredit", noOfRowsInSellerCredit + 1);
			} else {
				log.warn("Unhandled Transaction Type: " + transactionType + ", for account : "+ accountNumber);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private BasicDBList getOrderItemsFromMap(Map<String, BasicDBObject> map) {
		BasicDBList orderItems = new BasicDBList();
		for (Map.Entry<String, BasicDBObject> entry : map.entrySet()) {
			orderItems.add(entry.getValue());
		}
		return orderItems;
	}

	private Boolean checkIsLastRow(Exchange exchange) {
		JSONArray orderList = exchange.getProperty("orderList", JSONArray.class);
		int orderIndex = exchange.getProperty("orderIndex", Integer.class);
		if (orderIndex == orderList.length()) {
			return true;
		}
		return false;
	}

	private Object getSIAAmountObject(String amount, String currencyCode) {
		long SIAAmount = CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(amount));
		return CurrencyUtil.getAmountObject(SIAAmount, currencyCode);
	}

	private String getSettlementStatus(String paymentStatus) {
		if (paymentStatus.equals("Paid")) {
			return SIAOrderSettlementStatus.SETTLED.toString();
		}
		return SIAOrderSettlementStatus.NOT_SETTLED.toString();
	}
	
	private long getSettlementDate(String date) throws ParseException {
		DateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
		Date result = new Date();
		result = df.parse(date);
		return result.getTime() / 1000L;
	}
}