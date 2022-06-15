package com.sellinall.lazada.bl;

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

public class CheckIsOrderHasTrackingNumber implements Processor {
	static Logger log = Logger.getLogger(CheckIsOrderHasTrackingNumber.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONArray orderList = new JSONArray(exchange.getIn().getBody().toString());
		List<String> orderItemIDs = (List<String>) exchange.getProperty("orderItemIDs");
		ArrayList<String> orderItemIDsForGeneratingAWB = new ArrayList<String>();
		Map<String, JSONObject> orderMsgNeedToPublish = new HashMap<String, JSONObject>();
		boolean isNeedToGenerateAWB = false;
		for (int i = 0; i < orderList.length(); i++) {
			JSONObject order = orderList.getJSONObject(i);
			processOrderItems(orderItemIDs, orderItemIDsForGeneratingAWB, order, orderMsgNeedToPublish);
		}
		if (orderItemIDsForGeneratingAWB.size() > 0) {
			isNeedToGenerateAWB = true;
		}
		exchange.setProperty("orderItemIDsForGeneratingAWB", orderItemIDsForGeneratingAWB);
		exchange.setProperty("isNeedToGenerateAWB", isNeedToGenerateAWB);
		exchange.setProperty("orderMsgNeedToPublish", orderMsgNeedToPublish);
	}

	private void processOrderItems(List<String> orderItemIDs, ArrayList<String> orderItemIDsForGeneratingAWB,
			JSONObject order, Map<String, JSONObject> orderMsgNeedToPublish) throws JSONException {
		JSONArray orderItems = order.getJSONArray("orderItems");
		boolean isNeedToGenerateAWB = false;
		String orderID = order.getString("orderID");
		for (int i = 0; i < orderItems.length(); i++) {
			JSONObject orderItem = orderItems.getJSONObject(i);
			String orderItemID = orderItem.getString("orderItemID");
			if (orderItemIDs.contains(orderItemID)) {
				if (!orderItem.has("shippingTrackingDetails")
						|| !orderItem.getJSONObject("shippingTrackingDetails").has("airwayBill")
						|| orderItem.getJSONObject("shippingTrackingDetails").getString("airwayBill").isEmpty()) {
					orderItemIDsForGeneratingAWB.add(orderItemID);
					isNeedToGenerateAWB = true;
				}
			}
		}
		if (isNeedToGenerateAWB) {
			JSONObject site = order.getJSONObject("site");
			order.put("site", site.get("name"));
			order.put("nickNameID", site.get("nickNameID"));
			orderMsgNeedToPublish.put(orderID, order);
		}
	}

}
