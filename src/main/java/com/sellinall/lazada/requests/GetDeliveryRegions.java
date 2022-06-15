package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetDeliveryRegions implements Processor {
	static Logger log = Logger.getLogger(GetDeliveryRegions.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject userChannel = exchange.getProperty("userChannel", JSONObject.class);
		JSONObject freeShippingRegionData = new JSONObject();
		if (exchange.getProperties().containsKey("accessToken")) {
			String accessToken = exchange.getProperty("accessToken", String.class);
			JSONObject postHelper = userChannel.getJSONObject("postHelper");
			freeShippingRegionData = GetFreeShippingRegions(accessToken, postHelper.getString("hostURL"));
		} else {
			String failureReason = "Invalid access token";
			if (exchange.getProperties().containsKey("failureReason")) {
				failureReason = exchange.getProperty("failureReason", String.class);
			}
			freeShippingRegionData.put("status", "FAILED");
			freeShippingRegionData.put("failureReason", failureReason);
		}
		exchange.getOut().setBody(freeShippingRegionData);
	}

	public static JSONObject GetFreeShippingRegions(String accessToken, String hostURL) throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		JSONObject responseObj = new JSONObject();
		try {
			String response = NewLazadaConnectionUtil.callAPI(hostURL, "/promotion/freeshipping/regions/get",
					accessToken, map, "", "", "GET", clientID, clientSecret);
			JSONObject freeShippingRegionResponse = new JSONObject(response);
			if (freeShippingRegionResponse.getString("code").equals("0")) {
				responseObj.put("data", freeShippingRegionResponse.getJSONArray("data"));
				responseObj.put("status", "SUCCESS");
			} else {
				log.error("Getting failure response in free shipping regions, response :" + response);
				String failureReason = "Getting failure response from Api";
				if (freeShippingRegionResponse.has("message")) {
					failureReason = freeShippingRegionResponse.getString("message");
				}
				responseObj.put("status", "FAILED");
				responseObj.put("failureReason", failureReason);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return responseObj;
	}

}
