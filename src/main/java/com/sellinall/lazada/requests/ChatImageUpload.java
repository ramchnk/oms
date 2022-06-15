package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.DateUtil;

public class ChatImageUpload implements Processor {
	static Logger log = Logger.getLogger(ChatImageUpload.class.getName());

	public void process(Exchange exchange) throws Exception {

		String accessToken = (String) exchange.getProperty("accessToken");
		String accountNumber = (String) exchange.getProperty("accountNumber");
		String nickNameID = (String) exchange.getProperty("nickNameID");
		exchange.setProperty("isImageUpload", false);

		JSONObject msgContent = exchange.getProperty("content", JSONObject.class);
		String payload = createSingleImagePayload(msgContent.getString("imageUrl"));
		String requestBody = "payload=" + URLEncoder.encode(payload);

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("payload", payload);

		String url = (String) exchange.getProperty("hostURL");
		String clientID = Config.getConfig().getLazadaChatClientID();
		String clientSecret = Config.getConfig().getLazadaChatClientSecret();
		String result = NewLazadaConnectionUtil.callAPI(url, "/image/migrate", accessToken, map, requestBody, "",
				"POST", clientID, clientSecret);
		JSONObject channelResponse = new JSONObject(result);

		if (channelResponse.has("data")) {
			JSONObject body = channelResponse.getJSONObject("data");
			if (body.get("image") instanceof JSONObject) {
				exchange.setProperty("isImageUploaded", true);
				exchange.setProperty("uploadedImageURL", body.getJSONObject("image").getString("url"));
			}
		} else {
			log.error("lazada server not responding while uploading image for accountNumber : " + accountNumber
					+ " and nickNameID : " + nickNameID + " and response is: " + result.toString());
		}
	}

	private static String createSingleImagePayload(String imageURL) {
		String payload = "<?xml version='1.0' encoding='UTF-8' ?>";
		payload += "<Request>";
		payload += "<Image>";
		payload += "<Url>";
		payload += imageURL + "?" + (Long) DateUtil.getSIADateFormat();
		payload += "</Url>";
		payload += "</Image>";
		payload += "</Request>";
		return payload;
	}

}
