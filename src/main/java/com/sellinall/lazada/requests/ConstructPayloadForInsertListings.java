package com.sellinall.lazada.requests;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ConstructPayloadForInsertListings implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONArray inventoryList = new JSONArray(exchange.getIn().getBody().toString());
		JSONObject payload = new JSONObject();
		payload.put("newListing", false);
		if (inventoryList.length() > 1) {
			// variant listing
			payload.put("isValidParentSKU", false);
		} else {
			// non variant
			payload.put("isValidParentSKU", true);
		}
		if (exchange.getProperty("userId") != null) {
			payload.put("userId", exchange.getProperty("userId", String.class));
		}
		if (exchange.getProperty("userName") != null) {
			payload.put("userName", exchange.getProperty("userName", String.class));
		}
		payload.put("inventoryList", inventoryList);
		exchange.getOut().setBody(payload);
	}
}
