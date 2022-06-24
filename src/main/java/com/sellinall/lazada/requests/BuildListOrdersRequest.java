package com.sellinall.lazada.requests;

import java.net.URLEncoder;
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

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.DateUtil;

public class BuildListOrdersRequest implements Processor {
	static Logger log = Logger.getLogger(BuildListOrdersRequest.class.getName());
	static int maxRetryCount = 3;
	int retryCount = 1;

	public void process(Exchange exchange) throws Exception {
		exchange.setProperty("hasMoreRecords", false);
		exchange.setProperty("isOrderPollingScuccess", false);
		String pageOffSet = exchange.getProperty("pageOffSet", String.class);
		String limit = Config.getConfig().getOrderLimit();
		String accessToken = exchange.getProperty("accessToken", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("offset", pageOffSet);
		map.put("limit", limit);
		map.put("access_token", accessToken);
		map.put("sort_direction", "DESC");
		String queryParams = "&offset=" + pageOffSet + "&limit=" + limit + "&sort_direction=DESC";

		String fromTime = exchange.getProperty("from", String.class);
		fromTime += "T00:00:00+08:00";
		map.put("created_after", fromTime);
		map.put("sort_by", "created_at");
		queryParams += "&created_after=" + URLEncoder.encode(fromTime, "UTF-8") + "&sort_by=created_at";

		String response = "";

		// Paid orders API response
		response = callAPI(exchange, accessToken, map, queryParams, retryCount);

		if (exchange.getProperty("isOrderPollingScuccess", Boolean.class)) {
			response = processResponse(new JSONObject(response), exchange);

		}
		exchange.getOut().setBody(response);
	}

	private String callAPI(Exchange exchange, String accessToken, HashMap<String, String> map, String queryParams,
			int retryCount) throws JSONException, InterruptedException {
		String ScApiHost = exchange.getProperty("hostURL", String.class);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String requestType = exchange.getProperty("requestType", String.class);
		long startTime = System.currentTimeMillis();
		String response = NewLazadaConnectionUtil.callAPI(ScApiHost, "/orders/get", accessToken, map, "", queryParams,
				"GET", clientID, clientSecret);
		if (response.isEmpty()) {
			exchange.setProperty("isOrderPollingScuccess", false);
			long endTime = System.currentTimeMillis();
			log.error("Getting empty response in order polling for " + ", took : " + (endTime - startTime) + "ms");
			return "";
		}
		JSONObject responseObject = new JSONObject(response);
		if (!responseObject.getString("code").equals("0")) {
			exchange.setProperty("isOrderPollingScuccess", false);
			log.error("error occuring for accountNumber : " + exchange.getProperty("accountNumber") + ", nickNameID : "
					+ exchange.getProperty("nickNameID") + ", requestType : " + requestType + " & response : "
					+ response);
			return response;
		}
		JSONObject body = responseObject.getJSONObject("data");
		if (!body.has("orders")) {
			exchange.setProperty("isOrderPollingScuccess", false);
			log.error("Getting invalid response from order api for accountNumber : "
					+ exchange.getProperty("accountNumber") + ", nickNameID : " + exchange.getProperty("nickNameID")
					+ ", retryCount : " + retryCount + ", requestType : " + requestType + " & response : " + response);
			if (retryCount <= maxRetryCount) {
				retryCount++;
				Thread.sleep(1000);
				return callAPI(exchange, accessToken, map, queryParams, retryCount);
			}
			return response;
		}
		log.info("Got success response from get orders api for accountNumber : " + exchange.getProperty("accountNumber")
				+ ", nickNameID : " + exchange.getProperty("nickNameID") + ", requestType : " + requestType
				+ " & request_id : " + responseObject.getString("request_id"));
		exchange.setProperty("isOrderPollingScuccess", true);
		return response;
	}

	private String processResponse(JSONObject response, Exchange exchange) throws JSONException {
		JSONObject body = new JSONObject();
		JSONObject bodyUnpaid = new JSONObject();
		Boolean isBodyExits = false;
		Boolean isBodyUnpaidExits = false;
		int paidOrdersCount = 0;
		if (response.has("data")) {
			body = response.getJSONObject("data");
			if (body.has("count") && body.has("orders") && body.get("orders") instanceof JSONArray) {
				isBodyExits = true;
				paidOrdersCount = body.getInt("count");
			}
		}
		exchange.setProperty("paidOrdersCount", paidOrdersCount);

		if (isBodyUnpaidExits) {
			if (isBodyExits) {
				JSONArray orders = body.getJSONArray("orders");
				JSONArray ordersUnpaid = bodyUnpaid.getJSONArray("orders");
				for (int i = 0; i < ordersUnpaid.length(); i++) {
					orders.put(ordersUnpaid.getJSONObject(i));
				}
				body.put("count", body.getInt("count") + bodyUnpaid.getInt("count"));
				return response.toString();
			}
		}
		return response.toString();
	}
}