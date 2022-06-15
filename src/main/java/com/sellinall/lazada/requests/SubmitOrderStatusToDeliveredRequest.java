package com.sellinall.lazada.requests;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
import com.sellinall.util.enums.SIAShippingStatus;

public class SubmitOrderStatusToDeliveredRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitOrderStatusToDeliveredRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		callAPIToDelivered(exchange, 1);
	}

	private void callAPIToDelivered(Exchange exchange, int retryCount) throws IOException, JSONException {
		ArrayList<Long> deliverOrderItemID = exchange.getProperty("deliverOrderItemID", ArrayList.class);
		JSONObject order = (JSONObject) exchange.getProperty("order");
		String orderID = order.getString("orderID");
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		if (deliverOrderItemID.size() == 0) {
			exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
			return;
		}
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("order_item_ids", deliverOrderItemID.toString());
		String queryParams = "&order_item_ids=" + URLEncoder.encode(deliverOrderItemID.toString(), "UTF-8");
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = "";
		JSONObject serviceResponse = new JSONObject();
		try {
			response = NewLazadaConnectionUtil.callAPI(hostURL, "/order/sof/delivered", accessToken, map, "",
					queryParams, "POST", clientID, clientSecret);
			log.info("Delivery for order ID : " + orderID + " accountNumber : " + accountNumber
					+ " nickNameID :" + nickNameID + " and reponse :" + response + " & Request : " + map);
			 serviceResponse = new JSONObject(response);
			if (serviceResponse.has("code") && serviceResponse.getString("code").equals("0")) {
				exchange.setProperty("updateStatus", OrderUpdateStatus.COMPLETE.toString());
				// To mark as delivered in our system
				order.put("orderStatus", SIAOrderStatus.DELIVERED.toString());
				order.put("shippingStatus", SIAShippingStatus.DELIVERED.toString());
				updateOrderItemStatus(order, deliverOrderItemID);
				return;
			} else {
				if (serviceResponse.has("code")) {
					String code = serviceResponse.getString("code");
					if ((code.contains("ApiCallLimit") || code.contains("ServiceTimeout")) && retryCount < 3) {
						Thread.sleep(10000);
						retryCount++;
						callAPIToDelivered(exchange, retryCount);
						return;
					}
				}
			}

			log.error("Unable mark to delivery for order ID : " + orderID + " accountNumber: " + accountNumber
					+ " nickNameID:" + nickNameID + " and reponse:" + response);
			exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
			if(serviceResponse.has("message")) {
				exchange.setProperty("failureReason", serviceResponse.getString("message"));
			}
		} catch (Exception e) {
			log.error("Internal error occured for order ID : " + orderID + " accountNumber: " + accountNumber
					+ " nickNameID:" + nickNameID + " and response: " + response);
			e.printStackTrace();
			exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
			if(serviceResponse.has("message")) {
				exchange.setProperty("failureReason", serviceResponse.getString("message"));
			}
		}
	}

	private void updateOrderItemStatus(JSONObject order, ArrayList<Long> deliverOrderItemID) throws JSONException {
		JSONArray orderItems = order.getJSONArray("orderItems");
		Set<String> orderStatuses = new LinkedHashSet<String>();
		Set<String> shippingStatuses = new LinkedHashSet<String>();
		for (int i = 0; i < orderItems.length(); i++) {
			JSONObject orderItem = orderItems.getJSONObject(i);
			if (deliverOrderItemID.contains(Long.parseLong(orderItem.getString("orderItemID")))) {
				orderItem.put("orderStatus", SIAOrderStatus.DELIVERED.toString());
				orderItem.put("shippingStatus", SIAShippingStatus.DELIVERED.toString());
				orderStatuses.add(SIAOrderStatus.DELIVERED.toString());
				shippingStatuses.add(SIAShippingStatus.DELIVERED.toString());
			} else {
				orderStatuses.add(orderItem.getString("orderStatus"));
				shippingStatuses.add(orderItem.getString("shippingStatus"));
			}
		}
		order.put("orderStatuses", orderStatuses);
		order.put("shippingStatuses", shippingStatuses);
	}

}