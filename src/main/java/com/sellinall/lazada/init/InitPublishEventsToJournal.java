package com.sellinall.lazada.init;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;

public class InitPublishEventsToJournal implements Processor {

	public void process(Exchange exchange) throws Exception {
		BasicDBObject inventory = exchange.getProperty("skuDetails", BasicDBObject.class);
		String invSKU = inventory.getString("SKU");
		Map<String, JSONObject> skuFeedMap = exchange.getProperty("skuFeedMap", Map.class);
		JSONObject skuObject = new JSONObject();
		if (skuFeedMap.containsKey(invSKU)) {
			skuObject = skuFeedMap.get(invSKU);
			exchange.setProperty("SKU", skuObject.getString("SKU"));
			exchange.setProperty("individualListingSellerSKU", skuObject.getString("sellerSKU"));
			exchange.setProperty("impactSnapshot", skuObject);
			exchange.setProperty("request", skuObject);
		}
	}

}
