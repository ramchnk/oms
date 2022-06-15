package com.sellinall.lazada.requests;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.enums.OrderUpdateStatus;
import com.sellinall.util.enums.SIAOrderStatus;

public class SubmitOrderStatusToCanceledRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitOrderStatusToCanceledRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		callAPIToCancelOrder(exchange);
	}

	private void callAPIToCancelOrder(Exchange exchange) throws IOException, JSONException {
		ArrayList<String> orderItemIDs = exchange.getProperty("orderItemIDs", ArrayList.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		JSONObject order = (JSONObject) exchange.getProperty("order");
		boolean allCancelApiFailed = true;
		boolean allCancelApiSucessfullyPosted = true;
		for (String orderItemID : orderItemIDs) {
			map.put("order_item_id", orderItemID);
			map.put("access_token", accessToken);
			String queryParams = "";
			if (exchange.getProperties().containsKey("cancelReason")) {
				map.put("reason_detail", exchange.getProperty("cancelReason", String.class));
				queryParams = "&reason_detail="
						+ URLEncoder.encode(exchange.getProperty("cancelReason", String.class), "UTF-8");
			}
			if (exchange.getProperties().containsKey("cancelReasonID")) {
				map.put("reason_id", exchange.getProperty("cancelReasonID", String.class));
				queryParams += "&reason_id=" + exchange.getProperty("cancelReasonID", String.class);
			}
			queryParams += "&order_item_id=" + orderItemID;
			String clientID = Config.getConfig().getLazadaClientID();
			String clientSecret = Config.getConfig().getLazadaClientSecret();
			String response = "";
			try {
				response = NewLazadaConnectionUtil.callAPI(hostURL, "/order/cancel", accessToken, map, "",
						queryParams, "POST", clientID, clientSecret);
				JSONObject serviceResponse = new JSONObject(response);
				if (serviceResponse.has("code") && serviceResponse.getString("code").equals("0")) {
					allCancelApiFailed = false;
					updateCancelOrderItemStatus(order, orderItemID, true);
				} else {
					log.error("Response: " + response);
					allCancelApiSucessfullyPosted = false;
					updateCancelOrderItemStatus(order, orderItemID, false);
				}
			} catch (Exception e) {
				log.error("Exception ocurred during partial cancel for orderItemID: " + orderItemID
						+ ", accountNumber: " + exchange.getProperty("accountNumber", String.class) + ",nickNameID "
						+ exchange.getProperty("nickNameID", String.class) + ", request:" + queryParams
						+ " and response:" + response);
				e.printStackTrace();
				allCancelApiSucessfullyPosted = false;
				updateCancelOrderItemStatus(order, orderItemID, false);
			}
		}
		if (allCancelApiFailed) {
			exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
		} else {
			exchange.setProperty("updateStatus", OrderUpdateStatus.COMPLETE.toString());
			// set top level order cancelled status(if all orderItem
			// Status cancelled set top level also cancelled)
			updateOverallOrderCancelStatus(order, allCancelApiSucessfullyPosted);
		}
	}

	private void updateCancelOrderItemStatus(JSONObject order, String orderItemID, boolean isUpdated)
			throws JSONException {
		JSONArray orderItems = order.getJSONArray("orderItems");
		for (int i = 0; i < orderItems.length(); i++) {
			JSONObject orderItem = orderItems.getJSONObject(i);
			if (orderItemID.equalsIgnoreCase(orderItem.getString("orderItemID"))) {
				if (isUpdated) {
					orderItem.put("orderStatus", SIAOrderStatus.CANCELLED.toString());
				} else {
					orderItem.put("orderStatus", orderItem.getString("currentOrderStatus"));
				}
				orderItem.remove("currentOrderStatus");
				break;
			}
		}
	}

	private void updateOverallOrderCancelStatus(JSONObject order, boolean allCancelApiSucessfullyPosted)
			throws JSONException {
		JSONArray orderItems = order.getJSONArray("orderItems");
		Set<String> orderStatuses = new HashSet<String>();
		for (int i = 0; i < orderItems.length(); i++) {
			JSONObject orderItem = orderItems.getJSONObject(i);
			orderStatuses.add(orderItem.getString("orderStatus"));
		}
		order.put("orderStatuses", orderStatuses);
		if (allCancelApiSucessfullyPosted) {
			if (orderStatuses.size() == 1) {
				order.put("orderStatus", orderStatuses.iterator().next());
			}
		}
	}

}