package com.sellinall.lazada.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ConstructPayloadForAutoLinking implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONArray inventoryList = new JSONArray(exchange.getIn().getBody().toString());
		removeSKUFromChild(inventoryList);
		boolean isValidParentSKU = true;
		if (inventoryList.length() > 1) {
			isValidParentSKU = false; // variant listings
		}
		JSONObject payload = new JSONObject();
		payload.put("newListing", false);
		if (exchange.getProperty("userId") != null) {
			payload.put("userId", exchange.getProperty("userId", String.class));
		}
		if (exchange.getProperty("userName") != null) {
			payload.put("userName", exchange.getProperty("userName", String.class));
		}
		payload.put("isValidParentSKU", isValidParentSKU);
		payload.put("inventoryList", inventoryList);
		exchange.getOut().setBody(payload);
	}

	private static void removeSKUFromChild(JSONArray inventoryList) throws JSONException {
		if (inventoryList.length() > 1) {
			for (int i = 1; i < inventoryList.length(); i++) {
				JSONObject childObject = (JSONObject) inventoryList.get(i);
				childObject.remove("SKU");
				inventoryList.put(i, childObject);
			}
		}
	}

}
