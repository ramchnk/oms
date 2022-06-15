package com.sellinall.lazada.init;

import java.util.HashSet;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class InitializeGetSettlement implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject request = exchange.getIn().getBody(JSONObject.class);
		exchange.getOut().setBody(request);
		if (request.has("merchantID")) {
			exchange.setProperty("merchantID", request.getString("merchantID"));
		}
		exchange.setProperty("accountNumber", request.getString("accountNumber"));
		exchange.setProperty("nickNameID", request.getString("nickNameID"));
		if (request.has("nickName")) {
			exchange.setProperty("nickName", request.getString("nickName"));
		}
		// process initiated from Business center
		if (request.has("isSellerInitiated")) {
			exchange.setProperty("isSellerInitiated", request.getBoolean("isSellerInitiated"));
		}
		exchange.setProperty("documentID", request.getString("documentID"));
		exchange.setProperty("requestObj", request);
		// for get settlement details
		if (exchange.getProperty("requestType", String.class).equals("getSettlementDetails")) {
			exchange.setProperty("isLastPage", false);
			exchange.setProperty("noOfRows", 0);
			exchange.setProperty("noOfOrders", 0);
			exchange.setProperty("noOfOrderItems", 0);
			exchange.setProperty("noOfRowsProcessed", 0);
			exchange.setProperty("noOfOrdersProcessed", 0);
			exchange.setProperty("noOfOrderItemsProcessed", 0);
			exchange.setProperty("noOfOrdersSkipped", 0);
			exchange.setProperty("noOfOrderItemsSkipped", 0);
			exchange.setProperty("totalProcessedAmount", 0);

			exchange.setProperty("processedOrders", new HashSet<String>());
			exchange.setProperty("skippedOrders", new HashSet<String>());
			exchange.setProperty("processedOrderItems", new HashSet<String>());
			exchange.setProperty("skippedOrderItems", new HashSet<String>());
		}
	}

}
