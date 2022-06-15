package com.sellinall.lazada.requests;

import java.net.URLEncoder;
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

public class SubmitBulkPriceUpdateRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitBulkPriceUpdateRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		String responseString = "";
		String payload = exchange.getIn().getBody(String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		String url = exchange.getProperty("hostURL", String.class);
		map.put("access_token", accessToken);
		map.put("payload", payload);
		String requestBody = "payload=" + URLEncoder.encode(payload);
		log.debug("update item payLoad =" + payload);
		String apiName = "/product/price_quantity/update";
		try {
			String clientID = Config.getConfig().getLazadaClientID();
			String clientSecret = Config.getConfig().getLazadaClientSecret();
			responseString = NewLazadaConnectionUtil.callAPI(url, apiName, accessToken, map, requestBody, "", "POST",
					clientID, clientSecret);
			JSONObject response = new JSONObject(responseString);
			if (response.has("code") && !response.getString("code").equals("0")) {
				log.error("Unable update bulk price for occured while updating item for  "
						+ exchange.getProperty("sellerSKUList", List.class).toString() + " ,accountNumber : "
						+ exchange.getProperty("accountNumber", String.class) + " nickNameID : "
						+ exchange.getProperty("nickNameID", String.class) + " apiName : " + apiName
						+ " and response : " + response.toString());
				String failureMessage = "Failed to update price in lazada, Please try again after some time";
				if (response.has("message")) {
					failureMessage = response.getString("message");
				}
				exchange.setProperty("failureReason", failureMessage);
			} else {
				if (response.has("detail")) {
					JSONArray details = response.getJSONArray("detail");
					for (int i = 0; i < details.length(); i++) {
						JSONObject detail = details.getJSONObject(i);
						Map<String, String> sellerUpdateFailureMessageMap = exchange
								.getProperty("sellerUpdateFailureMessageMap", HashMap.class);
						sellerUpdateFailureMessageMap.put(detail.getString("seller_sku"), detail.getString("message"));
					}
				}
			}
		} catch (Exception e) {
			log.error("Exception occured while updating item for  "
					+ exchange.getProperty("sellerSKUList", List.class).toString() + " ,accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " apiName : " + apiName + " and response : "
					+ responseString);
			e.printStackTrace();
		}
	}

}