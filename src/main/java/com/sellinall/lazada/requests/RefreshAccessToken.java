package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.EncryptionUtil;

public class RefreshAccessToken implements Processor {
	static Logger log = Logger.getLogger(RefreshAccessToken.class.getName());

	public void process(Exchange exchange) throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String countryCode = exchange.getProperty("countryCode", String.class);
		String refreshToken = "";
		boolean isCommonRequest = false;
		if (exchange.getProperties().containsKey("isCommonRequest")) {
			isCommonRequest = exchange.getProperty("isCommonRequest", Boolean.class);
		}
		if (isCommonRequest) {
			refreshToken = Config.getConfig().getCommonRefreshToken(countryCode);
		} else if (exchange.getProperties().containsKey("appType")
				&& exchange.getProperty("appType", String.class).equals("chat")) {
			refreshToken = EncryptionUtil.decrypt(exchange.getProperty("chatRefreshToken", String.class));
		} else {
			refreshToken = EncryptionUtil.decrypt(exchange.getProperty("refreshToken", String.class));
		}
		map.put("refresh_token", refreshToken);
		String queryParams = "&refresh_token=" + refreshToken;
		String response = "", clientID = "", clientSecret = "";
		if (isCommonRequest) {
			clientID = Config.getConfig().getCommonClientID();
			clientSecret = Config.getConfig().getCommonClientSecret();
		} else if (exchange.getProperties().containsKey("appType")
				&& exchange.getProperty("appType", String.class).equals("chat")) {
			clientID = Config.getConfig().getLazadaChatClientID();
			clientSecret = Config.getConfig().getLazadaChatClientSecret();
		} else {
			clientID = Config.getConfig().getLazadaClientID();
			clientSecret = Config.getConfig().getLazadaClientSecret();
		}
		response = NewLazadaConnectionUtil.callAPI(Config.getConfig().getCompleteAuthUrl(), "/auth/token/refresh", null,
				map, "", queryParams, "POST", clientID, clientSecret);
		JSONObject responseObj = new JSONObject(response);
		if (!responseObj.has("access_token")) {
			log.error("access_token is not created and refresh access token response is: " + responseObj
					+ ", for accountNumber " + accountNumber + ": " + nickNameID);
			if (responseObj.has("code") && responseObj.getString("code").equals("IncompleteSignature")) {
				log.error("invalidSignature error map: " + map + " and queryParams: " + queryParams
						+ ", for accountNumber: " + accountNumber + "& nickNameID: " + nickNameID);
			}
			exchange.setProperty("failureReason", "Auth failure please check your credential");
			JSONObject responseData = new JSONObject();
			responseData.put("status", "failure");
			exchange.getOut().setBody(responseData);
			exchange.setProperty("stopProcess", true);
			return;
		}
		exchange.setProperty("userCredentials", responseObj);
		exchange.setProperty("accessToken", responseObj.getString("access_token"));
	}

}
