package com.sellinall.lazada.message;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.enums.SIAReconciliationStatus;



public class ConstructBatchMessageForReconciliation implements Processor {
	static Logger log = Logger.getLogger(ConstructBatchMessageForReconciliation.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject message = new JSONObject();
		String currencyCode = exchange.getProperty("currencyCode", String.class);
		message.put("accountNumber", exchange.getProperty("accountNumber"));
		message.put("reconciliationRecordObjectId", exchange.getProperty("reconciliationRecordObjectId"));
		if (!exchange.getProperty("failureReason", String.class).isEmpty()) {
			message.put("status", SIAReconciliationStatus.FAILED.toString());
			message.put("failureReason", exchange.getProperty("failureReason", String.class));
		} else {
			if (exchange.getProperties().containsKey("warning")){
				message.put("status", SIAReconciliationStatus.COMPLETED_WITH_WARNING.toString());
				message.put("warning", exchange.getProperty("warning", String.class));
			} else {
				message.put("status", SIAReconciliationStatus.COMPLETED.toString());
			}
			message.put("noOfOrders", exchange.getProperty("noOfOrders"));
			ArrayList<String> skippedOrders = (ArrayList<String>) exchange.getProperty("skippedOrders");
			message.put("noOfOrdersSkipped", skippedOrders.size());
			ArrayList<String> processedOrders = (ArrayList<String>) exchange.getProperty("processedOrders");
			message.put("noOfOrdersProcessed", processedOrders.size());
			BasicDBObject totalAmountObj = (BasicDBObject) CurrencyUtil
					.getAmountObject(exchange.getProperty("totalAmount"), currencyCode);
			message.put("totalAmount", LazadaUtil.parseToJsonObject((DBObject) totalAmountObj));
			BasicDBObject receivedAmountObj = (BasicDBObject) CurrencyUtil
					.getAmountObject(exchange.getProperty("receivedAmount"), currencyCode);
			message.put("receivedAmount", LazadaUtil.parseToJsonObject((DBObject) receivedAmountObj));
			BasicDBObject awaitingAmountObj = (BasicDBObject) CurrencyUtil
					.getAmountObject(exchange.getProperty("awaitingAmount"), currencyCode);
			message.put("awaitingAmount", LazadaUtil.parseToJsonObject((DBObject) awaitingAmountObj));
			BasicDBObject refundedAmountObj = (BasicDBObject) CurrencyUtil
					.getAmountObject(exchange.getProperty("refundedAmount"), currencyCode);
			message.put("refundedAmount", LazadaUtil.parseToJsonObject((DBObject) refundedAmountObj));
			message.put("skippedOrders", exchange.getProperty("skippedOrders"));
			message.put("processedOrders", exchange.getProperty("processedOrders"));
			BasicDBObject totalSellerCreditAmountObj = (BasicDBObject) CurrencyUtil
					.getAmountObject(exchange.getProperty("totalSellerCreditAmount"), currencyCode);
			message.put("totalSellerCreditAmount", LazadaUtil.parseToJsonObject((DBObject) totalSellerCreditAmountObj));
			BasicDBObject totalOtherFeeObj = (BasicDBObject) CurrencyUtil
					.getAmountObject(exchange.getProperty("totalOtherFee"), currencyCode);
			message.put("totalOtherFee", LazadaUtil.parseToJsonObject((DBObject) totalOtherFeeObj));
			message.put("noOfRowsInOtherFee", exchange.getProperty("noOfRowsInOtherFee"));
			message.put("noOfRowsInSellerCredit", exchange.getProperty("noOfRowsInSellerCredit"));
		}
		message.put("requestType", "reconciliationOrderUpdate");
		if (exchange.getProperties().containsKey("isAdmin")) {
			message.put("isAdmin", exchange.getProperty("isAdmin", boolean.class));
		}
		log.debug(message.toString());
		exchange.getOut().setBody(message);
	}

}
