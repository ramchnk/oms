package com.sellinall.lazada.response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ProcessVariantDetails implements Processor {
	static Logger log = Logger.getLogger(ProcessVariantDetails.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = new JSONObject(exchange.getIn().getBody(String.class));
		JSONObject products = inBody.getJSONObject("GetMatchingProductForIdResult").getJSONObject("Products");
		JSONObject product = products.getJSONObject("Product");
		if (product.has("Relationships") && (product.get("Relationships") instanceof JSONObject) ) {
			JSONObject relationShips = product.getJSONObject("Relationships");
			JSONArray variationChild = new JSONArray();
			if (relationShips.get("ns2:VariationChild") instanceof JSONArray) {
				variationChild = relationShips.getJSONArray("ns2:VariationChild");
			} else {
				variationChild.put(relationShips.getJSONObject("ns2:VariationChild"));
			}
			log.warn("VariationChild: " + variationChild);
			exchange.setProperty("VariationChild", variationChild);
		}
	}
}