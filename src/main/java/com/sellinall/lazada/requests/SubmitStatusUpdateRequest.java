package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class SubmitStatusUpdateRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitStatusUpdateRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		String failureReason = "";
		String response = null;
		String payload = exchange.getIn().getBody(String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String url = exchange.getProperty("hostURL", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("payload", payload);
		String requestBody = "payload=" + URLEncoder.encode(payload);
		log.debug("update status payLoad =" + payload);
		String apiName = "/product/update";
		try {
			String clientID = Config.getConfig().getLazadaClientID();
			String clientSecret = Config.getConfig().getLazadaClientSecret();
			response = NewLazadaConnectionUtil.callAPI(url, apiName, accessToken, map, requestBody, "", "POST",
					clientID, clientSecret);
			if (response.isEmpty()) {
				log.error("Getting empty response for SKU : " + exchange.getProperty("SKU")
						+ " apiName : " + apiName + " & payload : " + payload);
				exchange.setProperty("isPostingSuccess", false);
				failureReason = "Getting empty response while updating the status, please try again";
			} else {
				JSONObject channelResponse = new JSONObject(response);
				if (channelResponse.has("code") && channelResponse.getString("code").equals("0")) {
					exchange.setProperty("isPostingSuccess", true);
					log.info("Status update done for SKU:" + exchange.getProperty("SKU") + " apiName : " + apiName
							+ " , Update Item Response : " + response);
				} else {
					log.error("Failed to update status for SKU " + exchange.getProperty("SKU")
							+ " and its response is: " + channelResponse + " & payload : " + payload);
					exchange.setProperty("isPostingSuccess", false);
					failureReason = "Failure - " + getApiErrorMessage(channelResponse);
				}
			}
		} catch (Exception e) {
			exchange.setProperty("isPostingSuccess", false);
			failureReason = "Error occured while updating the status, please try again";
			e.printStackTrace();
		}
		Map<String, JSONObject> sellerSKUFeedMap = exchange.getProperty("sellerSKUFeedMap", LinkedHashMap.class);
		List<String> customSKUs = exchange.getProperty("invCustomSKUS", List.class);
		for (String customSKU : customSKUs) {
			if (sellerSKUFeedMap.containsKey(customSKU)) {
				JSONObject feedMessage = sellerSKUFeedMap.get(customSKU);
				if (failureReason.isEmpty()) {
					feedMessage.put("status", "success");
				} else {
					feedMessage.put("status", "failure");
					feedMessage.put("failureReason", failureReason);
				}
			}
		}
	}

	private String getApiErrorMessage(JSONObject channelResponse) {
		try {
			String errorMessage = "";
			if (channelResponse.has("detail")) {
				JSONArray details = channelResponse.getJSONArray("detail");
				for (int i = 0; i < details.length(); i++) {
					JSONObject detail = details.getJSONObject(i);
					if (detail.has("field")) {
						errorMessage += (errorMessage.isEmpty() ? "" : ",") + detail.getString("field") + ":"
								+ detail.getString("message");
					} else {
						errorMessage += (errorMessage.isEmpty() ? "" : ",") + detail.getString("message");
					}
				}
			}
			return channelResponse.getString("message") + (errorMessage.isEmpty() ? "" : "-") + errorMessage;
		} catch (Exception e) {
			log.error("Exception occured while parsing update product status response :" + channelResponse);
		}
		return "Error occured while updating product status, please try again";
	}

}
