package com.sellinall.lazada.init;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;

public class InitUpdateSKUDBQuery implements Processor {

	public void process(Exchange exchange) throws Exception {
		Map<String, JSONObject> skuFeedMap = exchange.getProperty("skuFeedMap", Map.class);
		Map<String, String> failureReasonMap = exchange.getProperty("failureReasonMap", Map.class);
		BasicDBObject inventory = exchange.getIn().getBody(BasicDBObject.class);
		String invSKU = inventory.getString("SKU");
		if (skuFeedMap.containsKey(invSKU)) {
			JSONObject skuObject = skuFeedMap.get(invSKU);
			exchange.setProperty("listingQuantities", skuObject.getJSONArray("listingQuantity"));
			exchange.setProperty("requestQuantities", skuObject.getJSONArray("quantities"));
		}
		if (failureReasonMap.containsKey(inventory.getString("customSKU"))) {
			exchange.setProperty("failureReason", failureReasonMap.get(inventory.getString("customSKU")));
		}
		exchange.setProperty("quantity", inventory.getInt("overAllQuantity"));
		exchange.setProperty("requestType", "quantityChange");
		exchange.setProperty("skuDetails", inventory);
		exchange.setProperty("SKU", invSKU);
		exchange.getOut().setBody(inventory);
	}

}
