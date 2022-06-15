package com.sellinall.lazada.bl;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetOrderDetails {

	static Logger log = Logger.getLogger(GetOrderDetails.class.getName());
	static final int MAX_RETRY_COUNT = 3;

	public static JSONObject getOrder(String hostUrl, String accountNumber, String nickNameId, String accessToken,
			String orderID, int retryCount) throws JSONException {
		String apiName = "/order/get";
		String queryParams = "&order_id=" + orderID;

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("order_id", orderID);

		JSONObject order = (JSONObject) callAPI(hostUrl, apiName, accountNumber, nickNameId, accessToken, map,
				queryParams, retryCount);
		return order;
	}

	public static JSONArray getOrderItems(String hostUrl, String accountNumber, String nickNameId, String accessToken,
			String orderID, int retryCount) throws JSONException {
		String apiName = "/order/items/get";
		String queryParams = "&order_id=" + orderID;

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("order_id", orderID);

		JSONArray orderItems = (JSONArray) callAPI(hostUrl, apiName, accountNumber, nickNameId, accessToken, map,
				queryParams, retryCount);
		return orderItems;
	}

	private static Object callAPI(String hostUrl, String apiName, String accountNumber, String nickNameId,
			String accessToken, HashMap<String, String> map, String queryParams, int retryCount) throws JSONException {
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();

		boolean retry = false;
		String response = "";
		long startTime = System.currentTimeMillis();
		try {
			response = NewLazadaConnectionUtil.callAPI(hostUrl, apiName, accessToken, map, "", queryParams, "GET",
					clientID, clientSecret);
			if (response.isEmpty()) {
				retry = true;
				long endTime = System.currentTimeMillis();
				log.error("Getting empty response from order details api for accountNumber : " + accountNumber
						+ ", nickNameID : " + nickNameId + ", orderID : " + map.get("order_id") + ", retryCount : "
						+ retryCount + ", took : " + (endTime - startTime) + "ms for api : " + apiName
						+ ", queryParams : " + queryParams + " & response : " + response);
			} else {
				JSONObject serviceResponse = new JSONObject(response);
				if (!serviceResponse.getString("code").equals("0") || !serviceResponse.has("data")) {
					retry = true;
					log.error("Getting invalid response from order details api for accountNumber : " + accountNumber
							+ ", nickNameID : " + nickNameId + ", orderID : " + map.get("order_id") + ", retryCount : "
							+ retryCount + " for api : " + apiName + ", queryParams : " + queryParams + " & response : "
							+ response);
				}
			}
			if (retry && retryCount <= MAX_RETRY_COUNT) {
				retryCount++;
				Thread.sleep(1000);
				return callAPI(hostUrl, apiName, accountNumber, nickNameId, accessToken, map, queryParams, retryCount);
			}
			JSONObject serviceResponse = new JSONObject(response);
			if (serviceResponse.has("data")) {
				log.info("Got success response from get order detail api for accountNumber : " + accountNumber
						+ ", nickNameID : " + nickNameId + ", queryParams : " + queryParams + " & request_id : "
						+ serviceResponse.getString("request_id"));
				return serviceResponse.get("data");
			}
		} catch (Exception e) {
			log.error("error occurred while accessing order details for accountNumber : " + accountNumber
					+ " and nickNameID : " + nickNameId + ", queryParams : " + queryParams + " and response is "
					+ response);
			e.printStackTrace();
		}
		if (apiName.equals("/order/items/get")) {
			return new JSONArray();
		} else {
			return new JSONObject();
		}
	}

}
