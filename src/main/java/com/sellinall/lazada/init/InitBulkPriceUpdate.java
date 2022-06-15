package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class InitBulkPriceUpdate implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		String accountNumber = inBody.getString("accountNumber");
		String nickNameID = inBody.getString("nickNameID");
		exchange.setProperty("accountNumber", accountNumber);
		exchange.setProperty("nickNameID", nickNameID);
		exchange.setProperty("addendum", inBody.getJSONObject("addendum"));
		Map<String, JSONObject> sellerSKUPriceDetailsMap = new HashMap<>();
		List<String> sellerSKUList = new ArrayList<>();
		JSONArray data = inBody.getJSONArray("data");
		for (int i = 0; i < data.length(); i++) {
			String sellerSKU = data.getJSONObject(i).getString("sellerSKU");
			sellerSKUList.add(sellerSKU);
			sellerSKUPriceDetailsMap.put(sellerSKU, data.getJSONObject(i));
		}
		exchange.setProperty("sellerSKUList", sellerSKUList);
		exchange.setProperty("sellerUpdateMessageMap", new HashMap<String,JSONArray>());
		exchange.setProperty("sellerUpdateFailureMessageMap", new HashMap<String, JSONArray>());
		exchange.setProperty("sellerSKUPriceDetailsMap", sellerSKUPriceDetailsMap);
		exchange.setProperty("requestType", "updatePrice");
	}

}
