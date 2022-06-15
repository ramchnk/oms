package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.util.JSON;

public class InitializeGetTrackingDetails implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject order = exchange.getProperty("order", JSONObject.class);
		if (order.has("buyerDetails")) {
			exchange.setProperty("buyerDetails", order.getJSONObject("buyerDetails"));
		}
		exchange.setProperty("orderItemList", (BasicDBList) JSON.parse(order.getJSONArray("orderItems").toString()));
		exchange.setProperty("accountNumber", order.getString("accountNumber"));
		JSONObject orderSoldAmount = new JSONObject();
		if (order.has("orderSoldAmount")) {
			orderSoldAmount = order.getJSONObject("orderSoldAmount");
		} else if(order.has("orderAmount")) {
			orderSoldAmount = order.getJSONObject("orderAmount");
			if (order.has("sellerDiscountAmount")) {
				JSONObject sellerDiscountAmount = order.getJSONObject("sellerDiscountAmount");
				long amount = orderSoldAmount.getLong("amount") - sellerDiscountAmount.getLong("amount");
				orderSoldAmount.put("amount", amount);
			}
		}
		exchange.setProperty("orderSoldAmount", orderSoldAmount);
	}
}
