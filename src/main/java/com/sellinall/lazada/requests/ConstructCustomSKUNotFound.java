package com.sellinall.lazada.requests;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

public class ConstructCustomSKUNotFound implements Processor {

	public void process(Exchange exchange) throws Exception {
		if (exchange.getProperties().containsKey("isInventoryListEmpty")
				&& exchange.getProperty("isInventoryListEmpty", Boolean.class)) {
			List<String> customSKUs = exchange.getProperty("customSKUs", List.class);
			exchange.setProperty("isCustomSKUsNotFound", true);
			exchange.setProperty("Action", "ProductStatusUpdate");
			constructCustomSKUNotFoundMessage(exchange, customSKUs);
			return;
		}

	}

	private void constructCustomSKUNotFoundMessage(Exchange exchange, List<String> notFoundSKUs) throws JSONException {
		Map<String, JSONObject> sellerSKUFeedMap = exchange.getProperty("sellerSKUFeedMap", LinkedHashMap.class);
		List<BasicDBObject> customSKUNotMessages = new ArrayList<BasicDBObject>();
		for (String customSKU : notFoundSKUs) {
			if (sellerSKUFeedMap.containsKey(customSKU)) {
				BasicDBObject feedMessage = (BasicDBObject) JSON.parse(sellerSKUFeedMap.get(customSKU).toString());
				if (feedMessage.containsField("rowIdentifier")) {
					customSKUNotMessages.add(feedMessage);
				}
			}
		}
		exchange.setProperty("customSKUNotMessages", customSKUNotMessages);
	}

}
