package com.sellinall.lazada.requests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ConstructCustomSKUBatchMessage implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		int index = exchange.getProperty("CamelLoopIndex", Integer.class);
		Map<String, JSONObject> sellerSKUFeedMap = exchange.getProperty("sellerSKUFeedMap", Map.class);
		List<String> customSKUs = (List<String>) exchange.getProperty("customSKUs");
		List<String> notFoundSKUsList = new ArrayList<String>();
		if (exchange.getProperties().containsKey("notFoundSKUsList")) {
			notFoundSKUsList = (List<String>) exchange.getProperty("notFoundSKUsList");
		}
		boolean isBulkStatusUpdate = false;
		String customSKU = customSKUs.get(index);
		JSONObject skuObject = new JSONObject();
		if (sellerSKUFeedMap.containsKey(customSKU)) {
			skuObject = sellerSKUFeedMap.get(customSKU);
		}
		if (notFoundSKUsList.contains(customSKU)) {
			skuObject.put("failureReason", "Seller SKU not found");
			skuObject.put("status", "failure");
		}
		if (skuObject.has("rowIdentifier")) {
			isBulkStatusUpdate = true;
			exchange.setProperty("isEligibleToPublishBatchMsg", true);
			exchange.setProperty("SKUMap", skuObject);
			exchange.getOut().setBody(skuObject);
		} else {
			exchange.setProperty("isEligibleToPublishBatchMsg", false);
		}

		boolean isEligibleToUpdateStock = false;
		if (skuObject.has("needToUpdateStock") && skuObject.getString("updateToStatus").equals("active")) {
			isEligibleToUpdateStock = Boolean.parseBoolean(skuObject.getString("needToUpdateStock"));
		}
		if (isEligibleToUpdateStock && (!skuObject.has("status") || skuObject.getString("status").equals("failure"))) {
			isEligibleToUpdateStock = false;
		}
		if (!isBulkStatusUpdate && isEligibleToUpdateStock && skuObject.getString("requestType").equals("updateItem")) {
			// updateItem requestType feed only come here
			// stock update will happen for this custom sku list
			JSONArray customSKUList = new JSONArray();
			customSKUList.put(skuObject.getString("customSKU"));
			exchange.setProperty("customSKUList", customSKUList);
		}
		exchange.setProperty("isBulkStatusUpdate", isBulkStatusUpdate);
		exchange.setProperty("isEligibleToUpdateStock", isEligibleToUpdateStock);
	}

}
