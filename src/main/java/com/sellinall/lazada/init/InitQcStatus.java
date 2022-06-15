package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class InitQcStatus implements Processor {

	public void process(Exchange exchange) throws Exception {
		List<JSONObject> responseObjList = exchange.getIn().getBody(ArrayList.class);
		List<String> refrenceIDList = new ArrayList<String>();
		exchange.setProperty("isEligibleToUpdateQCStatus", false);
		Map<String, JSONObject> refrenceIDMap = new HashMap<String, JSONObject>();
		for (int i = 0; i < responseObjList.size(); i++) {
			JSONObject responseObj = responseObjList.get(i);
			String status = "";
			if (responseObj.has("status")) {
				status = responseObj.getString("status");
			} else {
				status = responseObj.getString("Status");
			}
			if (status.equals("pending")) {
				continue;
			}
			String sellerSKU = "";
			String failureReason = "";
			if (responseObj.has("seller_sku")) {
				sellerSKU = responseObj.getString("seller_sku");
			} else {
				sellerSKU = responseObj.getString("SellerSKU");
			}
			JSONObject refrenceIDObj = new JSONObject();
			refrenceIDObj.put("status", status);
			refrenceIDObj.put("sellerSKU", sellerSKU);
			if (status.equals("rejected")) {
				if (responseObj.has("reason")) {
					failureReason = responseObj.getString("reason");
				} else if (responseObj.has("Reason")) {
					failureReason = responseObj.getString("Reason");
				} else {
					failureReason = "Unable to fetch failure reason, Please check from seller center";
				}
				refrenceIDObj.put("failureReason", failureReason);
			} else {
				refrenceIDList.add(sellerSKU);
			}
			refrenceIDMap.put(sellerSKU, refrenceIDObj);
		}
		if(!refrenceIDList.isEmpty()) {
			exchange.setProperty("approvedRefrenceIDList", refrenceIDList);
		}
		exchange.setProperty("refrenceIDMap", refrenceIDMap);
	}

}
