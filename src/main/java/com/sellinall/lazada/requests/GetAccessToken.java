package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetAccessToken implements Processor {
	static Logger log = Logger.getLogger(GetAccessToken.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		HashMap<String, String> map = new HashMap<String, String>();
		String authToken = inBody.getString("authToken");
		map.put("code", authToken);
		String payload = "code=" + authToken;
		String clientID = "";
		String clientSecret = "";

		clientID = Config.getConfig().getLazadaClientID();
		clientSecret = Config.getConfig().getLazadaClientSecret();

		String response = NewLazadaConnectionUtil.callAPI(Config.getConfig().getCompleteAuthUrl(), "/auth/token/create",
				null, map, payload, "", "POST", clientID, clientSecret);

		JSONObject responseObj = new JSONObject(response);
		if (!responseObj.has("access_token")) {
			log.error("Failed to get access token, request : " + payload + " & response : " + responseObj);
			exchange.setProperty("failureReason", "Auth failure please check your credential");
			return;
		}
		if (!responseObj.has("country_user_info") && responseObj.has("country_user_info_list")) {
			responseObj.put("country_user_info", responseObj.get("country_user_info_list"));
		}
		boolean isGlobalAccount = false;
		if (responseObj.has("country") && responseObj.getString("country").equalsIgnoreCase("cb")) {
			isGlobalAccount = true;
			log.info("Global account found for accountNumber : " + exchange.getProperty("accountNumber")
					+ " & country user info : " + responseObj.get("country_user_info"));
		}
		exchange.setProperty("isGlobalAccount", isGlobalAccount);
		exchange.setProperty("userCredentials", responseObj);
		exchange.setProperty("isValidAccount", true);
		
	}
}
