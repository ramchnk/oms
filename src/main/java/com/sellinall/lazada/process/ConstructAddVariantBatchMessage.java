package com.sellinall.lazada.process;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ConstructAddVariantBatchMessage implements Processor {

	public void process(Exchange exchange) throws Exception {
		List<String> variantsList = exchange.getProperty("variantsList", ArrayList.class);
		JSONObject rowIdentifier = exchange.getProperty("rowIdentifier" ,JSONObject.class);
		JSONArray siteNicknames = exchange.getProperty("siteNicknames", JSONArray.class);
		List<JSONObject> SKUMaps = new ArrayList<JSONObject>();
		String failureReason = "";
		String status = "failure";
		if (exchange.getProperty("failureReason") != null) {
			failureReason = exchange.getProperty("failureReason", String.class);
		}
		if (failureReason.isEmpty()) {
			status = "success";
		}
		for (String variantSKU : variantsList) {
			JSONObject json = new JSONObject();
			json.put("SKU", variantSKU);
			json.put("rowIdentifier", rowIdentifier);
			json.put("siteNicknames", siteNicknames);
			json.put("status", status);
			if (status.equals("failure")) {
				json.put("failureReason", failureReason);
			}
			SKUMaps.add(json);
		}
		exchange.getOut().setBody(SKUMaps);
	}

}
