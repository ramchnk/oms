package com.sellinall.lazada.requests;

import java.io.IOException;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class SubmitIndividualOrderRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitIndividualOrderRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject order = getOrderDetail(exchange);
		exchange.getOut().setBody(order);
	}

	private JSONObject getOrderDetail(Exchange exchange) throws JSONException, IOException {
		DBObject order = exchange.getIn().getBody(DBObject.class);
		DBObject site = (DBObject) order.get("site");
		order.put("site", site.get("name"));
		order.put("nickNameID", site.get("nickNameID"));
		String ScApiHost = exchange.getProperty("hostURL", String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String orderID = order.get("orderID").toString();
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("order_id", orderID);
		map.put("access_token", accessToken);
		String queryParams = "&order_id=" + orderID;
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = NewLazadaConnectionUtil.callAPI(ScApiHost, "/order/items/get", accessToken, map, "",
				queryParams, "GET", clientID, clientSecret);

		JSONObject orderResponse = new JSONObject(response);
		exchange.setProperty("isUpdateUnpaidOrders", false);
		if (response.isEmpty()) {
			log.error("Getting empty response for nickNameId: " + nickNameID + " and for accountNumber: "
					+ accountNumber);
			return null;
		} else if (!orderResponse.has("data")) {
			log.error("Error ocurred for nickNameId: " + nickNameID + " and for accountNumber: " + accountNumber
					+ " and response is: " + orderResponse);
			return null;
		}

		JSONObject ordersFromDB = LazadaUtil.parseToJsonObject(order);
		JSONArray orderResponseArray = new JSONArray();
		if (orderResponse.get("data") instanceof JSONObject) {
			orderResponseArray.put(0, orderResponse.get("data"));
		} else {
			orderResponseArray = orderResponse.getJSONArray("data");
		}
		setOrderStatus(orderResponseArray, ordersFromDB, exchange);
		return ordersFromDB;
	}

	private void setOrderStatus(JSONArray orderResponseArray, JSONObject ordersFromDB, Exchange exchange)
			throws JSONException, IOException {

		JSONArray orderItemsFromDB = (JSONArray) ordersFromDB.get("orderItems");
		JSONArray statuses = new JSONArray();
		// payment Method is passed as empty to set paymentStatus as
		// Not_Initiated in LazadaUtil
		String paymentMethod = "";
		for (int i = 0; i < orderResponseArray.length(); i++) {
			JSONArray orderStatus = new JSONArray();
			JSONObject orderResponseObject = orderResponseArray.getJSONObject(i);
			if (orderResponseObject.has("status")) {
				String status = orderResponseObject.getString("status");
				statuses.put(status);
				// checks if any item from response is cancelled within 35 mins
				if (orderResponseObject.getString("status").equals("canceled")) {
					for (int j = 0; j < orderItemsFromDB.length(); j++) {
						JSONObject orderItemFromDB = (JSONObject) orderItemsFromDB.get(j);
						// Iterate the corresponding orderItem from DB which got
						// canceled response from lazada
						if (orderItemFromDB.get("orderItemID")
								.equals(orderResponseObject.get("order_item_id").toString())) {
							orderStatus.put(status);
							exchange.setProperty("isUpdateUnpaidOrders", true);
							if (orderResponseObject.has("reason")) {
								exchange.setProperty("lazadaCancelReason", orderResponseObject.getString("reason"));
							}
							LazadaUtil.getStatusDetails(orderStatus, paymentMethod, orderItemsFromDB.getJSONObject(j),
									ordersFromDB, exchange, "orderItems");
							break;
						}
					}
				}
			}
		}
		LazadaUtil.getStatusDetails(statuses, paymentMethod, ordersFromDB, ordersFromDB, exchange, "order");
	}
}
