package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class BuildOrderDetailsRequest implements Processor {
	static Logger log = Logger.getLogger(BuildOrderDetailsRequest.class.getName());
	final int maxRetryCount = 3;

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		log.debug("GetOrderDetails in body: " + inBody);
		boolean needToUpdateDocumnetUrl = false;
		exchange.setProperty("order", inBody);
		String ScApiHost = exchange.getProperty("hostURL", String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String countryCode = exchange.getProperty("countryCode", String.class);
		exchange.setProperty("timeOrderCreated", LazadaUtil.getOrderTimeValues(inBody, "created_at", countryCode));
		exchange.setProperty("timeOrderUpdated", LazadaUtil.getOrderTimeValues(inBody, "updated_at", countryCode));
		String queryParams = "";
		exchange.setProperty("apiName", "/order/items/get");
		HashMap<String, String> map = new HashMap<String, String>();
		if (inBody.has("order_id")) {
			String orderID = inBody.getString("order_id");
			exchange.setProperty("orderID", orderID);
			map.put("order_id", orderID);
			queryParams += "&order_id=" + orderID;
		} else if (exchange.getProperties().containsKey("orderIDs")) {
			JSONArray orderIDs = exchange.getProperty("orderIDs", JSONArray.class);
			exchange.setProperty("apiName", "/orders/items/get");
			map.put("order_ids", orderIDs.toString());
			queryParams += "&order_ids=" + URLEncoder.encode(orderIDs.toString());
		}
		if (inBody.has("statuses")) {
			JSONArray orderDetails = inBody.getJSONArray("statuses");
			for (int index = 0; index < orderDetails.length(); index++) {
				if (orderDetails.get(index).equals("repacked")) {
					needToUpdateDocumnetUrl = true;
				}
			}
		}
		map.put("access_token", accessToken);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		int retryCount = 1;
		exchange.setProperty("needToUpdateDocumnetUrl", needToUpdateDocumnetUrl);
		callAPI(exchange, ScApiHost, accessToken, map, queryParams, clientID, clientSecret, retryCount);
	}

	private void callAPI(Exchange exchange, String scApiHost, String accessToken, HashMap<String, String> map,
			String queryParams, String clientID, String clientSecret, int retryCount) throws JSONException {
		boolean retry = false;
		String response = "";
		long startTime = System.currentTimeMillis();
		String requestType = "";
		if (exchange.getProperties().containsKey("requestType")) {
			requestType = exchange.getProperty("requestType", String.class);
		}
		try {
			response = NewLazadaConnectionUtil.callAPI(scApiHost, exchange.getProperty("apiName", String.class), accessToken, map, "", queryParams,
					"GET", clientID, clientSecret);
			if (response.isEmpty()) {
				retry = true;
				long endTime = System.currentTimeMillis();
				log.error("Getting empty response from order details api for accountNumber : "
						+ exchange.getProperty("accountNumber") + ", nickNameID : " + exchange.getProperty("nickNameID")
						+ ", orderID : " + map.get("order_id") + ", retryCount : " + retryCount + ", took : "
						+ (endTime - startTime) + "ms & response : " + response + ", requestType : " + requestType);
			} else {
				JSONObject serviceResponse = new JSONObject(response);
				if (!serviceResponse.getString("code").equals("0") || !serviceResponse.has("data")) {
					retry = true;
					log.error("Getting invalid response from order details api for accountNumber : "
							+ exchange.getProperty("accountNumber") + ", nickNameID : "
							+ exchange.getProperty("nickNameID") + ", orderID : " + map.get("order_id")
							+ ", retryCount : " + retryCount + " & response : " + response + ", requestType : "
							+ requestType);
				}
			}
			if (retry && retryCount <= maxRetryCount) {
				retryCount++;
				Thread.sleep(1000);
				callAPI(exchange, scApiHost, accessToken, map, queryParams, clientID, clientSecret, retryCount);
			}
			JSONObject serviceResponse = new JSONObject(response);
			exchange.setProperty("orderItemResponse", serviceResponse.get("data"));
			exchange.getOut().setBody(serviceResponse.get("data"));
			exchange.setProperty("responseSuccess", true);
			log.info("Got success response from get order detail api for accountNumber : "
					+ exchange.getProperty("accountNumber") + ", nickNameID : " + exchange.getProperty("nickNameID")
					+ " & request_id : " + serviceResponse.getString("request_id") + ", requestType : " + requestType);
		} catch (Exception e) {
			log.error("error occurred while accessing order details for accountNumber : "
					+ exchange.getProperty("accountNumber") + " and nickNameID : " + exchange.getProperty("nickNameID")
					+ " orderID : " + map.get("order_id") + "and response is " + response + ", requestType : "
					+ requestType);
			e.printStackTrace();
		}
	}
}