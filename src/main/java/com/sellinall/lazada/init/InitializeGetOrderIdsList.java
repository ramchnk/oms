package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class InitializeGetOrderIdsList implements Processor {
	static Logger log = Logger.getLogger(InitializeGetOrderIdsList.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject request = exchange.getIn().getBody(JSONObject.class);
		log.debug("InBody: " + request);
		exchange.setProperty("accountNumber", request.get("accountNumber"));
		exchange.setProperty("nickNameID", request.get("nickNameID"));
		if (request.get("apiName").equals("getOrderItemDetails")) {
			exchange.setProperty("apiName", "/order/items/get");
		} else if (request.get("apiName").equals("getOrderDetails")) {
			exchange.setProperty("apiName", "/order/get");
		} else if (request.get("apiName").equals("getItemDetailBySellerSku")
				|| request.get("apiName").equals("getItemDetailByItemId")) {
			exchange.setProperty("apiName", "/product/item/get");
		}
		if (request.has("isSyncMissingOrder")) {
			exchange.setProperty("isSyncMissingOrder", request.getBoolean("isSyncMissingOrder"));
		}
		// orderID only is enough for getOrderItemDetails and getOrderDetails
		if (request.has("orderID")) {
			exchange.setProperty("orderID", request.getString("orderID"));
		} else if (request.has("itemID")) {
			exchange.setProperty("itemID", request.getString("itemID"));
		} else if (request.has("sellerSKU")) {
			exchange.setProperty("sellerSKU", request.getString("sellerSKU"));
		} else if (request.has("orderIDList")) {
			JSONArray orderIDListArray = request.getJSONArray("orderIDList");
			List<String> orderIDList = new ArrayList<String>();
			for (int i = 0; i < orderIDListArray.length(); i++) {
				orderIDList.add(orderIDListArray.getString(i));
			}
			exchange.setProperty("orderIDList", orderIDList);
		}
	}

}
