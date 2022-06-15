package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class SubmitDeactiveStatusRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitDeactiveStatusRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		String response = null;
		String payload = exchange.getIn().getBody(String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String sellerSKU = "";
		if (exchange.getProperties().containsKey("refrenceID")) {
			sellerSKU = exchange.getProperty("refrenceID", String.class);
		}
		String url = exchange.getProperty("hostURL", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("apiRequestBody", payload);
		String requestBody = "apiRequestBody=" + URLEncoder.encode(payload);
		String apiName = "/product/deactivate";
		try {
			String clientID = Config.getConfig().getLazadaClientID();
			String clientSecret = Config.getConfig().getLazadaClientSecret();
			response = NewLazadaConnectionUtil.callAPI(url, apiName, accessToken, map, requestBody, "", "POST",
					clientID, clientSecret);
			if (response.isEmpty()) {
				exchange.setProperty("isPostingSuccess", false);
				log.error("Getting empty response for " + exchange.getProperty("SKU") + ", sellerSKU:" + sellerSKU
						+ " apiName : " + apiName);
				exchange.setProperty("failureReason", "Getting empty response while deactivate the product, please try again");
			}
			JSONObject channelResponse = new JSONObject(response);
			if (channelResponse.has("code") && channelResponse.getString("code").equals("0")) {
				exchange.setProperty("isPostingSuccess", true);
				exchange.setProperty("isEligibleToUpdatePM", true);
				log.info("Deactivate product done for SKU:" + exchange.getProperty("SKU") + ", sellerSKU:"
						+ sellerSKU + " apiName : " + apiName + " , Update Item Response : " + response);
				return;
			}
			exchange.setProperty("isPostingSuccess", false);
			log.error("Failed to deactivate the product for " + exchange.getProperty("SKU") + " and its response is: "
					+ channelResponse + " & payload : " + payload);
			exchange.setProperty("failureReason", "Failure - " + getApiErrorMessage(channelResponse));
		} catch (Exception e) {
			log.error("Exception occured while deactivate SKU : " + exchange.getProperty("SKU")
					+ " ,accountNumber : " + exchange.getProperty("accountNumber", String.class) + " nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " apiName : " + apiName + " and response : "
					+ response);
			e.printStackTrace();
			exchange.setProperty("isPostingSuccess", false);
			exchange.setProperty("failureReason", "Error occured while deactivate the product - " + e.getMessage());
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
			log.error("Exception occured while parsing deactivate response :" + channelResponse);
		}
		return "Error occured while deactivate the product.";
	}

}
