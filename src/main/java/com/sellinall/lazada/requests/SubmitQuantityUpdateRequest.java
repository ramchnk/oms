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
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class SubmitQuantityUpdateRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitQuantityUpdateRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		Map<String, List<String>> itemIDMap = exchange.getProperty("itemIDMap", Map.class);
		Map<String, String> failureReasonMap = new HashMap<String, String>();
		String payload = exchange.getIn().getBody(String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String url = exchange.getProperty("hostURL", String.class);
		String response = null;
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("payload", payload);
		String requestBody = "payload=" + URLEncoder.encode(payload);
		log.debug("update item payLoad =" + payload);
		String apiName = "/product/stock/sellable/update";
		try {
			if (exchange.getProperties().containsKey("isQuantityDiff")
					&& exchange.getProperty("isQuantityDiff", Boolean.class)) {
				apiName = "/product/stock/sellable/adjust";
			}
			String clientID = Config.getConfig().getLazadaClientID();
			String clientSecret = Config.getConfig().getLazadaClientSecret();
			response = NewLazadaConnectionUtil.callAPI(url, apiName, accessToken, map, requestBody, "", "POST",
					clientID, clientSecret);
			if (response.isEmpty()) {
				log.error("Get empty reponse for apiName : " + apiName + " & payload : " + payload);
				exchange.setProperty("isStockUpdateSuccess", false);
				exchange.setProperty("failureReason",
						"Getting empty response while updating the stock, please try again");
			} else {
				JSONObject channelResponse = new JSONObject(response);
				getApiErrorMessage(channelResponse, failureReasonMap, itemIDMap);
				if (channelResponse.has("code") && channelResponse.getString("code").equals("0")) {
					exchange.setProperty("isStockUpdateSuccess", true);
					log.info("Stock update done. payload for apiName : " + apiName + " is " + payload
							+ " , and it's Response : " + response);
				} else {
					log.error("Stock update failed. It's response is: " + channelResponse + " & payload : " + payload);
					exchange.setProperty("isStockUpdateSuccess", false);
				}
			}
		} catch (Exception e) {
			log.info("Exception occured while updating stock. payload for apiName : " + apiName + " is " + payload
					+ " , and it's Response : " + response);
			e.printStackTrace();
			exchange.setProperty("failureReason", "Error occured while updating the stock, please try again");
		}
		exchange.setProperty("failureReasonMap", failureReasonMap);
	}

	private void getApiErrorMessage(JSONObject channelResponse, Map<String, String> failureReasonMap,
			Map<String, List<String>> itemIDMap) {
		try {
			if (channelResponse.has("detail")) {
				JSONArray details = channelResponse.getJSONArray("detail");
				for (int i = 0; i < details.length(); i++) {
					JSONObject detail = details.getJSONObject(i);
					if (detail.has("seller_sku") && detail.has("message")) {
						String skuID = detail.getString("seller_sku");
						String message = detail.getString("message");
						if (message.equals("ITEM_NOT_FOUND")) {
							if (itemIDMap.containsKey(skuID)) {
								List<String> customSKUs = itemIDMap.get(skuID);
								for (String customSKU : customSKUs) {
									failureReasonMap.put(customSKU, message);
								}
							}
						} else {
							failureReasonMap.put(skuID, message);
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Exception occured while parsing stock update response :" + channelResponse);
		}
	}

}
