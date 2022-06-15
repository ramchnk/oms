package com.sellinall.lazada.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.SIAShippingStatus;

public class ConstructUpdateOrderMessage implements Processor {
	static Logger log = Logger.getLogger(ConstructUpdateOrderMessage.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accountNumber = (String) exchange.getProperty("accountNumber");
		JSONObject order = (JSONObject) exchange.getProperty("order");
		JSONObject shippingDetails = order.getJSONObject("shippingDetails");
		JSONObject shippingTrackingDetails = new JSONObject();
		if (shippingDetails.has("shippingTrackingDetails")) {
			shippingTrackingDetails = shippingDetails.getJSONObject("shippingTrackingDetails");
		}
		shippingTrackingDetails.put("courierName", exchange.getProperty("shippingProviderType"));
		if (exchange.getProperties().containsKey("isAutoPackEnabled")
				&& exchange.getProperty("isAutoPackEnabled", Boolean.class)) {
			order.put("isAutoPackOrder", true);
			shippingTrackingDetails.put("courierName", exchange.getProperty("preferredLogistic"));
		} else {
			order.put("isAutoAcceptOrder", true);
		}
		shippingDetails.put("shippingTrackingDetails", shippingTrackingDetails);
		order.put("shippingDetails", shippingDetails);
		order.put("accountNumber", accountNumber);
		order = getOrderStatus(order, exchange);
		order.put("updateStatus", "COMPLETE");
		order.put("needToUpdateOrder", true);
		order.put("needToUpdateShipping", true);
		order.put("needToUpdatePayment", false);
		order.put("needToUpdateFeedBack", false);
		if (exchange.getProperties().containsKey("isDeliveredBySellerEnabled")) {
			order.put("isDeliveredBySellerEnabled", exchange.getProperty("isDeliveredBySellerEnabled", Boolean.class));
		}
		order.put("requestType", "updateOrder");
		log.info("Update order automation Message for account : " + exchange.getProperty("accountNumber")
				+ " orderID : " + exchange.getProperty("orderID") + " & publishing because of requestType : "
				+ exchange.getProperty("requestType", String.class));
		exchange.getOut().setBody(order);
		log.debug("created update order automation message");
	}

	private static JSONObject getOrderStatus(JSONObject order, Exchange exchange) throws JSONException {
		String state = (String) exchange.getProperty("toState");
		exchange.setProperty("isTransitionValid", false);
		if ((state != null && state.equals(SIAShippingStatus.READY_TO_SHIP.toString()))
				|| (exchange.getProperties().containsKey("isAutoPackEnabled")
						&& exchange.getProperty("isAutoPackEnabled", Boolean.class))) {
			// Maintains on exchange current order and shipping if auto accept
			// failure set this status
			order.put("currentOrderStatus", order.getString("orderStatus"));
			if (order.has("shippingStatus")) {
				order.put("currentShippingStatus", order.getString("shippingStatus"));
			}
			if (order.has("orderStatuses")) {
				order.put("currentOrderStatuses", order.getJSONArray("orderStatuses"));
			}
			if (order.has("shippingStatuses")) {
				order.put("currentShippingStatuses", order.getJSONArray("shippingStatuses"));
			}
			JSONArray orderItems = order.getJSONArray("orderItems");
			JSONObject currentItemStatusObj = LazadaUtil.getCurrentStatusObj(orderItems);
			order.put("currentOrderItemStatusObj", currentItemStatusObj);

			if (state != null) {
				/* Note: for auto pack no need to change status */
				order.put("toState", state);
				order.put("orderStatus", SIAOrderStatus.ACCEPTED);
				order.put("shippingStatus", SIAShippingStatus.READY_TO_SHIP);
			}
			exchange.setProperty("isTransitionValid", true);
			if (exchange.getProperties().containsKey("isNewOrder")) {
				order.put("isNewOrder", exchange.getProperty("isNewOrder", Boolean.class));
			}
		} else {
			log.error("automation for this shipping status is not handled, for account : "
					+ exchange.getProperty("accountNumber") + ", orderID " + exchange.getProperty("orderID"));
			// remove this if all the shipping statuses are handled.
		}
		return order;
	}

}
