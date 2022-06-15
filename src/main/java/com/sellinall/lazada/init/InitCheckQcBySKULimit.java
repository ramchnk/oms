package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class InitCheckQcBySKULimit implements Processor {
	static Logger log = Logger.getLogger(InitCheckQcBySKULimit.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("accountNumber", inBody.getString("accountNumber"));
		exchange.setProperty("nickNameID", inBody.getString("nickNameID"));
		/*TODO: need to remove, once QC polling issue resolved*/
		exchange.setProperty("refrenceIDMap", new HashMap<String, JSONObject>());
		constructRefrenceIDList(inBody, exchange);
	}

	private void constructRefrenceIDList(JSONObject inBody, Exchange exchange) throws JSONException {
		JSONArray skuAndRefrenceIdList = new JSONArray(inBody.getString("skuAndRefrenceIdList"));
		Map<String, String> skuAndRefrenceIdMap = new HashMap<String, String>();
		List<String> refrenceIDList = new ArrayList<String>();
		for (int index = 0; index < skuAndRefrenceIdList.length(); index++) {
			JSONObject skuAndRefrenceId = new JSONObject(skuAndRefrenceIdList.getString(index));
			String refrenceID = skuAndRefrenceId.getString("refrenceID");
			refrenceIDList.add(refrenceID);
			skuAndRefrenceIdMap.put(refrenceID, skuAndRefrenceId.getString("SKU"));
		}
		exchange.setProperty("refrenceIDList", refrenceIDList);
		exchange.setProperty("skuAndRefrenceIdMap", skuAndRefrenceIdMap);
	}
}
