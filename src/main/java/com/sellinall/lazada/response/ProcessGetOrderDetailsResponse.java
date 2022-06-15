package com.sellinall.lazada.response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;

public class ProcessGetOrderDetailsResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessGetOrderDetailsResponse.class.getName());

	public void process(Exchange exchange) throws Exception {
		Object response = exchange.getIn().getBody(Object.class);
		JSONObject responseObj = new JSONObject();
		if (response instanceof JSONArray) {
			JSONArray responseArr = new JSONArray(response.toString());
			JSONArray responseOrderItems = new JSONArray();
			for (int i = 0; i < responseArr.length(); i++) {
				JSONObject order = responseArr.getJSONObject(i);
				if (order.has("order_items")) {
					JSONArray orderItems = order.getJSONArray("order_items");
					for (int j = 0; j < orderItems.length(); j++) {
						JSONObject orderItem = orderItems.getJSONObject(j);
						JSONObject resOrderItem = new JSONObject();
						if (orderItem.has("order_id")) {
							resOrderItem.put("orderID", orderItem.getString("order_id"));
						}
						if (orderItem.has("order_item_id")) {
							resOrderItem.put("orderItemID", orderItem.getString("order_item_id"));
						}
						if (orderItem.has("status")) {
							resOrderItem.put("status", LazadaUtil.getSIAOrderStatus(orderItem.getString("status")));
						}
						responseOrderItems.put(resOrderItem);
					}
				}
			}
			responseObj.put("data", responseOrderItems);
			if (exchange.getProperties().containsKey("hasMoreRecords")) {
				responseObj.put("hasMoreRecords", exchange.getProperty("hasMoreRecords", Boolean.class));
			}
		} else {
			responseObj.put("data", new JSONArray());
		}
		exchange.getOut().setBody(responseObj);
	}

}
