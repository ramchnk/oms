package com.sellinall.lazada.init;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;

public class InitializeUpdateOrderRoute implements Processor {
	static Logger log = Logger.getLogger(InitializeUpdateOrderRoute.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject order = exchange.getIn().getBody(JSONObject.class);
		exchange.getOut().setBody(order);
		order.remove("requestType");
		boolean isDeliveredBySellerEnabled = false;
		if (order.has("isDeliveredBySellerEnabled")) {
			isDeliveredBySellerEnabled = order.getBoolean("isDeliveredBySellerEnabled");
		}
		exchange.setProperty("isDeliveredBySellerEnabled", isDeliveredBySellerEnabled);
		exchange.setProperty("notificationFrom", order.getString("site"));
		exchange.setProperty("order", order);
		exchange.setProperty("accountNumber", order.getString("accountNumber"));
		exchange.setProperty("nickNameID", order.getString("nickNameID"));
		exchange.setProperty("channelName", order.getString("site"));
		exchange.setProperty("orderID", order.getString("orderID"));
		exchange.setProperty("updatedOrderItems", new ArrayList<String>());
		exchange.setProperty("updateFailedOrderItems", new ArrayList<String>());

		/* Note: below fields are needed for auto pack/accept case */
		boolean isAutoPackOrder = false, isAutoAcceptOrder = false;
		if (order.has("isAutoPackOrder")) {
			isAutoPackOrder = order.getBoolean("isAutoPackOrder");
		}
		if (order.has("isAutoAcceptOrder")) {
			isAutoAcceptOrder = order.getBoolean("isAutoAcceptOrder");
		}
		exchange.setProperty("isAutoPackOrder", isAutoPackOrder);
		exchange.setProperty("isAutoAcceptOrder", isAutoAcceptOrder);
		if (order.has("isNewOrder")) {
			exchange.setProperty("isNewOrder", order.getBoolean("isNewOrder"));
		}
		if (order.has("toState")) {
			exchange.setProperty("toState", order.getString("toState"));
		}
		if (order.has("currentOrderStatus")) {
			exchange.setProperty("currentOrderStatus", order.getString("currentOrderStatus"));
		}
		if (order.has("currentShippingStatus")) {
			exchange.setProperty("currentShippingStatus", order.getString("currentShippingStatus"));
		}
		if (order.has("currentOrderStatuses")) {
			exchange.setProperty("currentOrderStatuses", order.getJSONArray("currentOrderStatuses"));
		}
		if (order.has("currentShippingStatuses")) {
			exchange.setProperty("currentShippingStatuses", order.getJSONArray("currentShippingStatuses"));
		}
		if (order.has("currentOrderItemStatusObj")) {
			exchange.setProperty("currentOrderItemStatusObj", order.getJSONObject("currentOrderItemStatusObj"));
		} else {
			JSONObject currentItemStatusObj = LazadaUtil.getCurrentStatusObj(order.getJSONArray("orderItems"));
			exchange.setProperty("currentOrderItemStatusObj", currentItemStatusObj);
		}
	}
}