package com.sellinall.lazada.message;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.enums.OrderUpdateStatus;

public class PreparePublishMessageForPNQ implements Processor {
	static Logger log = Logger.getLogger(PreparePublishMessageForPNQ.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject order = (JSONObject) exchange.getProperty("order");
		order.remove("requestType");
		if (exchange.getProperties().containsKey("updateStatus")) {
			if(exchange.getProperty("updateStatus", String.class).equals(OrderUpdateStatus.FAILED.toString())) {
				if(exchange.getProperties().containsKey("failureReason")) {
					order.put("failureReason",exchange.getProperty("failureReason", String.class));
				}
			}

			List<String> updatedOrderItems = null, updateFailedOrderItems = null;
			if (exchange.getProperties().containsKey("updatedOrderItems")) {
				updatedOrderItems = exchange.getProperty("updatedOrderItems", List.class);
			}
			if (exchange.getProperties().containsKey("updateFailedOrderItems")) {
				updateFailedOrderItems = exchange.getProperty("updateFailedOrderItems", List.class);
			}
			String updateStatus = (updatedOrderItems != null && updatedOrderItems.size() > 0)
					? OrderUpdateStatus.COMPLETE.toString()
					: exchange.getProperty("updateStatus", String.class);

			order.put("updateStatus", updateStatus);
			updateOrderItemStatus(exchange, order, updateStatus, updatedOrderItems, updateFailedOrderItems);
			if(exchange.getProperties().containsKey("orderStatusMovedTo")){
				order.put("orderStatus", exchange.getProperty("orderStatusMovedTo", String.class));
			}
		} else {
			// Some Order status we are not handled so we have directly udpate
			// DB
			order.put("updateStatus", OrderUpdateStatus.COMPLETE.toString());
		}
		if (exchange.getProperties().containsKey("isAutoAcceptOrder")
				&& exchange.getProperty("isAutoAcceptOrder", Boolean.class)) {
			order.put("addendum", LazadaUtil.buildOrderAddendumObj(true));
		}
		log.debug("order:" + order);
		exchange.getOut().setBody(order);
	}
	
	public void updateOrderItemStatus(Exchange exchange, JSONObject order, String updateStatus,
			List<String> updatedOrderItems, List<String> updateFailedOrderItems) throws JSONException {
		ArrayList<String> orderItemIDs = exchange.getProperty("orderItemIDs", ArrayList.class);
		ArrayList<String> freeGiftIDs = exchange.getProperty("freeGiftIDs", ArrayList.class);
		boolean isAnyItemCompleted = false;
		/* use of isAnyItemCompleted
		 * if any one order item update status is completed then the free gift update status also set as completed
		 * */
		if (order.has("orderItems")) {
			JSONArray orderItems = order.getJSONArray("orderItems");
			for (int i = 0; i < orderItems.length(); i++) {
				String orderItemId = orderItems.getJSONObject(i).getString("orderItemID");
				if (updatedOrderItems != null && updatedOrderItems.contains(orderItemId)) {
					orderItems.getJSONObject(i).put("updateStatus", OrderUpdateStatus.COMPLETE.toString());
					isAnyItemCompleted = true;
				} else if (updateFailedOrderItems != null && updateFailedOrderItems.contains(orderItemId)) {
					orderItems.getJSONObject(i).put("updateStatus", OrderUpdateStatus.FAILED.toString());
				} else if (orderItemIDs.contains(orderItemId)) {
					if(updateStatus.equals(OrderUpdateStatus.COMPLETE.toString())) {
						isAnyItemCompleted = true;
					}
					orderItems.getJSONObject(i).put("updateStatus", updateStatus);
				} else if (freeGiftIDs.contains(orderItemId)) {
					if (isAnyItemCompleted) {
						orderItems.getJSONObject(i).put("updateStatus", OrderUpdateStatus.COMPLETE.toString());
					} else {
						orderItems.getJSONObject(i).put("updateStatus", OrderUpdateStatus.FAILED.toString());
					}
				}
			}
		}

	}
}