package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class InitializeGetOrdersDetails implements Processor {
	static Logger log = Logger.getLogger(InitializeGetOrdersDetails.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		if (inBody.has("hasMoreRecords")) {
			exchange.setProperty("hasMoreRecords", inBody.getBoolean("hasMoreRecords"));
		}
		JSONArray orderIDs = new JSONArray();
		if (inBody.has("status") && inBody.getString("status").equals("failure")) {
			return;
		} else if (inBody.has("data")) {
			JSONObject data = inBody.getJSONObject("data");
			if (data.has("orders")) {
				JSONArray orders = data.getJSONArray("orders");
				for (int i = 0; i < orders.length(); i++) {
					JSONObject order = (JSONObject) orders.get(i);
					String orderID = order.getString("order_id");
					if (orderID.contains("-")) {
						log.error("invalid order found : " + order);
					} else {
						orderIDs.put(orderID);
					}
				}
			}
		}
		if (orderIDs.length() > 0) {
			exchange.setProperty("orderIDs", orderIDs);
		} else {
			JSONObject responseObj = new JSONObject();
			responseObj.put("data", new JSONArray());
			exchange.getOut().setBody(responseObj);
		}
	}

}
