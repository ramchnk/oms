package com.sellinall.lazada.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class ProcessAddItemToFlexiCombo implements Processor {

	public void process(Exchange exchange) throws Exception {
		exchange.setProperty("isAddFlexiComboItemFlow", true);
		if (!exchange.getProperties().containsKey("addFailureReason")) {
			Map<String, String> SKUIDMap = exchange.getProperty("SKUIDMap", HashMap.class);
			if (exchange.getProperties().containsKey("responseData")) {
				JSONObject responseData = exchange.getProperty("responseData", JSONObject.class);
				List<String> addFailedSKUs = getFailedSKUList(SKUIDMap, responseData);
				if (addFailedSKUs.size() > 0) {
					exchange.setProperty("addFailedSKUs", addFailedSKUs);
				}
			}
		}
	}

	private static List<String> getFailedSKUList(Map<String, String> SKUIDMap, JSONObject responceData) {
		List<String> failedSKUs = new ArrayList<String>();
		Iterator<String> iterator = responceData.keys();
		//In responceData only contains failure SKU's when adding flexiCombo Item
		while (iterator.hasNext()) {
			String key = (String) iterator.next();
			if (SKUIDMap.containsKey(key)) {
				failedSKUs.add(SKUIDMap.get(key));
			}
		}
		return failedSKUs;
	}

}
