package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class DocumentRequest implements Processor {
	static Logger log = Logger.getLogger(DocumentRequest.class.getName());
	static int maxRetryCount = 3;
	int retryCount = 1;

	public void process(Exchange exchange) throws Exception {
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		String documentType = exchange.getProperty("documentType", String.class);
		String orderIDs = "";

		orderIDs = exchange.getProperty("orderItemIDs", String.class);

		map.put("doc_type", documentType);
		map.put("order_item_ids", orderIDs);
		map.put("access_token", accessToken);
		String queryParams = "&doc_type=" + documentType + "&order_item_ids=" + URLEncoder.encode(orderIDs, "UTF-8");
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		JSONObject response = callAPI(exchange, hostURL, accessToken, map, queryParams, clientID, clientSecret,
				retryCount);
		exchange.getOut().setBody(response);
	}

	private JSONObject callAPI(Exchange exchange, String hostURL, String accessToken, HashMap<String, String> map,
			String queryParams, String clientID, String clientSecret, int retryCount)
			throws JSONException, org.codehaus.jettison.json.JSONException {
		JSONObject response = new JSONObject();
		String responseMsg = null;
		try {
			responseMsg = NewLazadaConnectionUtil.callAPI(hostURL, "/order/document/get", accessToken, map, "",
					queryParams, "GET", clientID, clientSecret);
			JSONObject serviceResponse = new JSONObject(responseMsg);
			if (serviceResponse.has("code") && serviceResponse.getString("code").equals("0")) {
				response.put("response", "success");
				JSONObject data = serviceResponse.getJSONObject("data");
				ArrayList<JSONObject> documentList = new ArrayList<JSONObject>();
				if (data.get("document") instanceof JSONObject) {
					documentList.add(data.getJSONObject("document"));
				} else {
					JSONArray list = data.getJSONArray("document");
					// Here need to check with multiple documents
					for (int i = 0; i <= list.length(); i++) {
						documentList.add(list.getJSONObject(i));
					}
				}
				response.put("documents", documentList);
				return response;
			} else {
				log.error("Getting invalid response from order document api for accountNumber : "
						+ exchange.getProperty("accountNumber") + ", nickNameID : " + exchange.getProperty("nickNameID")
						+ ", retryCount : " + retryCount + " & response : " + serviceResponse + " & queryParams : "
						+ queryParams);
				if (retryCount <= maxRetryCount) {
					retryCount++;
					Thread.sleep(1000);
					return callAPI(exchange, hostURL, accessToken, map, queryParams, clientID, clientSecret,
							retryCount);
				}
			}

		} catch (Exception e) {
			log.error("Error occurred while request document for accountNumber : "
					+ exchange.getProperty("accountNumber") + " and nickNameID : " + exchange.getProperty("nickNameID")
					+ " and response is " + responseMsg + " & queryParams : " + queryParams);
			e.printStackTrace();
		}
		response.put("response", "failure");
		response.put("failureReason", "Please try again after some time");
		return response;
	}
}
