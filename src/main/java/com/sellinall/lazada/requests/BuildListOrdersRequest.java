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
		String requestType = exchange.getProperty("requestType", String.class);
		exchange.setProperty("hasMoreRecords", false);
		exchange.setProperty("isOrderPollingScuccess", false);
		String pageOffSet = exchange.getProperty("pageOffSet", String.class);
		String limit = Config.getConfig().getOrderLimit();
		String accessToken = exchange.getProperty("accessToken", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("offset", pageOffSet);
		map.put("limit", limit);
		map.put("access_token", accessToken);
		map.put("sort_direction", "ASC");
		String queryParams = "&offset=" + pageOffSet + "&limit=" + limit + "&sort_direction=ASC";
		if (requestType.equals("checkMissingOrders")) {
			String countryCode = exchange.getProperty("countryCode", String.class);
			if (exchange.getProperties().containsKey("fromDate")) {
				String lastScannedTime = DateUtil.getDateFromSIAFormat(exchange.getProperty("fromDate", Long.class),
						"yyyy-MM-dd'T'HH:mm:ssXXX", LazadaUtil.timeZoneCountryMap.get(countryCode));
				exchange.setProperty("lastScannedTime", lastScannedTime);
				map.put("update_after", lastScannedTime);
				map.put("sort_by", "updated_at");
				queryParams += "&update_after=" + URLEncoder.encode(lastScannedTime, "UTF-8") + "&sort_by=updated_at";
			}
			if (exchange.getProperties().containsKey("toDate")) {
				String toScannedTime = DateUtil.getDateFromSIAFormat(exchange.getProperty("toDate", Long.class),
						"yyyy-MM-dd'T'HH:mm:ssXXX", LazadaUtil.timeZoneCountryMap.get(countryCode));
				map.put("update_before", toScannedTime);
				queryParams += "&update_before=" + URLEncoder.encode(toScannedTime, "UTF-8");
			}
		} else if (requestType.equals("scanNewOrders")) {
			String lastScannedTime = exchange.getProperty("lastScannedTime", String.class);
			map.put("created_after", lastScannedTime);
			map.put("sort_by", "created_at");
			queryParams += "&created_after=" + URLEncoder.encode(lastScannedTime, "UTF-8") + "&sort_by=created_at";
		} else {
			String countryCode = exchange.getProperty("countryCode", String.class);
			String lastScannedTime = exchange.getProperty("lastScannedTime", String.class);
			long lastRequestEndTime = exchange.getProperty("lastRequestEndTime", Long.class);
			String strLastRequestEndTime = DateUtil.getDateFromSIAFormat(lastRequestEndTime, "yyyy-MM-dd'T'HH:mm:ssXXX",
					LazadaUtil.timeZoneCountryMap.get(countryCode));

			map.put("update_after", lastScannedTime);
			map.put("update_before", strLastRequestEndTime);
			map.put("sort_by", "updated_at");
			queryParams += "&update_after=" + URLEncoder.encode(lastScannedTime, "UTF-8") + "&update_before="
					+ URLEncoder.encode(strLastRequestEndTime, "UTF-8") + "&sort_by=updated_at";
		}
		boolean callPaidOrdersAPI = true;
		boolean callUnPaidOrdersAPI = true;
		if (exchange.getProperties().containsKey("callPaidOrdersAPI")) {
			callPaidOrdersAPI = exchange.getProperty("callPaidOrdersAPI", Boolean.class);
		}
		if (exchange.getProperties().containsKey("callUnPaidOrdersAPI")) {
			callUnPaidOrdersAPI = exchange.getProperty("callUnPaidOrdersAPI", Boolean.class);
		}
		String response = "";
		if (callPaidOrdersAPI) {
			//Paid orders API response
			response = callAPI(exchange, accessToken, map, queryParams, retryCount);
			orderPollingLog(response, exchange, "");
		}
		String responseUnpaid = "";
		if ((exchange.getProperty("isOrderPollingScuccess", Boolean.class) || !callPaidOrdersAPI) && callUnPaidOrdersAPI) {
			map.put("status", "unpaid");
			queryParams += "&status=unpaid";
			//Unpaid orders API response
			responseUnpaid = callAPI(exchange, accessToken, map, queryParams, retryCount);
			orderPollingLog(response, exchange, "");
		}
		if (exchange.getProperty("isOrderPollingScuccess", Boolean.class)) {
			if(callPaidOrdersAPI && callUnPaidOrdersAPI) {
				response = processResponse(new JSONObject(response), new JSONObject(responseUnpaid), exchange);
			} else if (callPaidOrdersAPI && !callUnPaidOrdersAPI) {
				response = processResponse(new JSONObject(response), new JSONObject(), exchange);
			} else if (!callPaidOrdersAPI && callUnPaidOrdersAPI) {
				response = processResponse(new JSONObject(), new JSONObject(responseUnpaid), exchange);
			}
		}
		exchange.getOut().setBody(response);
	}
	
	private void orderPollingLog(String response, Exchange exchange, String paidStatus) throws JSONException {
		JSONObject serviceResponse = new JSONObject(response);
		if (!serviceResponse.getString("code").equals("0")) {
			return;
		}
		JSONObject data = serviceResponse.getJSONObject("data");
		if (data.has("orders") && data.get("orders") instanceof JSONArray) {
			int count = 0;
			String strLastRequestEndTime = null;
			String pollingMessage = null;
			try {
				Map<String, List<String>> statusOrdersMap = new HashMap<String, List<String>>();
				String lastScannedTime = exchange.getProperty("lastScannedTime", String.class);
				long lastRequestEndTime = exchange.getProperty("lastRequestEndTime", Long.class);
				String countryCode = exchange.getProperty("countryCode", String.class);
				String accountNumber = exchange.getProperty("accountNumber", String.class);
				String nickNameID = exchange.getProperty("nickNameID", String.class);
				count = data.getInt("count");
				strLastRequestEndTime = DateUtil.getDateFromSIAFormat(lastRequestEndTime, "yyyy-MM-dd'T'HH:mm:ssXXX",
						LazadaUtil.timeZoneCountryMap.get(countryCode));
				pollingMessage = count + (paidStatus.isEmpty() ? "" : " " + paidStatus) + " order(s) found from : "
						+ lastScannedTime + " to : " + strLastRequestEndTime + " for accountNumber: " + accountNumber
						+ " nickNameID: " + nickNameID + ", requestType : "
						+ exchange.getProperty("requestType", String.class) + ", requestID : "
						+ serviceResponse.getString("request_id");
				JSONArray orders = data.getJSONArray("orders");
				for (int i = 0; i < orders.length(); i++) {
					JSONObject order = orders.getJSONObject(i);
					String status = order.getJSONArray("statuses").getString(0);
					String orderNumber = order.getString("order_number");
					String createdAt = "";
					if (order.has("created_at")) {
						createdAt = " created " + order.getString("created_at");
					}
					String updatedAt = "";
					if (order.has("updated_at")) {
						updatedAt = " updated " + order.getString("updated_at");
					}
					String orderUpdates = orderNumber + createdAt + updatedAt;
					if (statusOrdersMap.get(status) != null) {
						List<String> orderList = statusOrdersMap.get(status);
						orderList.add(orderUpdates);
					} else {
						List<String> orderList = new ArrayList<String>();
						orderList.add(orderUpdates);
						statusOrdersMap.put(status, orderList);
					}
				}
				log.info(pollingMessage + " statusOrdersMap: " + statusOrdersMap);
			} catch (JSONException e) {
				log.error("Exception occured during order polling and count: " + pollingMessage);
				e.printStackTrace();
			}
		}
	}

	private String callAPI(Exchange exchange, String accessToken, HashMap<String, String> map, String queryParams,
			int retryCount) throws JSONException, InterruptedException {
		String ScApiHost = exchange.getProperty("hostURL", String.class);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String requestType = exchange.getProperty("requestType", String.class);
		long startTime = System.currentTimeMillis();
		String response = NewLazadaConnectionUtil.callAPI(ScApiHost, "/orders/get", accessToken, map, "", queryParams, "GET", clientID,
				clientSecret);
		if (response.isEmpty()) {
			exchange.setProperty("isOrderPollingScuccess", false);
			long endTime = System.currentTimeMillis();
			log.error("Getting empty response in order polling for accountNumber : "
					+ exchange.getProperty("accountNumber") + ", nickNameID : " + exchange.getProperty("nickNameID")
					+ ", requestType : " + requestType + ", took : " + (endTime - startTime) + "ms");
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

	private String processResponse(JSONObject response, JSONObject responseUnpaid, Exchange exchange) throws JSONException {
		JSONObject body = new JSONObject();
		JSONObject bodyUnpaid = new JSONObject();
		Boolean isBodyExits = false;
		Boolean isBodyUnpaidExits = false;
		int paidOrdersCount = 0;
		int unpaidOrdersCount = 0;
		if (response.has("data")) {
			body = response.getJSONObject("data");
			if (body.has("count") && body.has("orders") && body.get("orders") instanceof JSONArray) {
				isBodyExits = true;
				paidOrdersCount = body.getInt("count");
			}
		}
		if (responseUnpaid.has("data")) {
			bodyUnpaid = responseUnpaid.getJSONObject("data");
			if (bodyUnpaid.has("count") && bodyUnpaid.has("orders") && bodyUnpaid.get("orders") instanceof JSONArray) {
				isBodyUnpaidExits = true;
				unpaidOrdersCount = bodyUnpaid.getInt("count");
			}
		}
		exchange.setProperty("paidOrdersCount", paidOrdersCount);
		exchange.setProperty("unpaidOrdersCount", unpaidOrdersCount);
		if (isBodyUnpaidExits) {
			if (isBodyExits) {
				JSONArray orders = body.getJSONArray("orders");
				JSONArray ordersUnpaid = bodyUnpaid.getJSONArray("orders");
				for (int i = 0; i < ordersUnpaid.length(); i++) {
					orders.put(ordersUnpaid.getJSONObject(i));
				}
				body.put("count", body.getInt("count") + bodyUnpaid.getInt("count"));
				return response.toString();
			} else {
				return responseUnpaid.toString();
			}
		}
		return response.toString();
	}
}