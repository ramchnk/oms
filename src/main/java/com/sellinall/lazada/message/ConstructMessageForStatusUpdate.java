package com.sellinall.lazada.message;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ConstructMessageForStatusUpdate implements Processor {

	public void process(Exchange exchange) throws Exception {
		int index = exchange.getProperty("CamelLoopIndex", Integer.class);
		JSONObject inputRequest = exchange.getProperty("inputRequest", JSONObject.class);
		List<String> skuList = exchange.getProperty("skuListToProcess", List.class);
		String sku = skuList.get(index);
		boolean isChildVariantStatusUpdate = false;
		if (sku.contains("-")) {
			isChildVariantStatusUpdate = true;
		}
		exchange.setProperty("isChildVariantStatusUpdate", isChildVariantStatusUpdate);
		exchange.setProperty("SKU", sku);
		JSONArray siteNicknames = new JSONArray();
		siteNicknames.put(exchange.getProperty("nickNameID", String.class));
		JSONObject skuMap = new JSONObject();
		skuMap.put("SKU", sku);
		skuMap.put("rowIdentifier", inputRequest.getJSONObject("rowIdentifier"));
		skuMap.put("siteNicknames", siteNicknames);
		exchange.setProperty("SKUMap", skuMap);
		exchange.getOut().setBody(skuMap);
	}

}
