package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.APIUrlConfig;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetConversationDetails implements Processor {
	static Logger log = Logger.getLogger(GetConversationDetails.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accessToken = exchange.getProperty("accessToken", String.class);
		String countryCode = exchange.getProperty("countryCode", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("session_id", exchange.getProperty("conversationID", String.class));
		headers.put("access_token", accessToken);

		String url = APIUrlConfig.getConfig().getNewAPIUrl(countryCode);
		String queryParam = "&session_id=" + exchange.getProperty("conversationID", String.class);
		JSONObject payload = new JSONObject();
		try {
			String result = NewLazadaConnectionUtil.callAPI(url, "/im/session/get", accessToken, headers, "",
					queryParam, "GET", Config.getConfig().getLazadaChatClientID(),
					Config.getConfig().getLazadaChatClientSecret());
			JSONObject response = new JSONObject(result);
			if (response.has("data")) {
				payload = response.getJSONObject("data");
				if (payload.has("error") && !payload.getString("error").isEmpty()) {
					log.error("Error occurred while getting Conversation Details for accountNumber : " + accountNumber
							+ " and nickNameID : " + nickNameID + " and queryParam is " + queryParam
							+ " and response is " + response.toString());
				} else {
					payload.put("status", "success");
					exchange.getOut().setBody(payload);
				}
			}
		} catch (Exception e) {
			log.error("Internal error occurred while getting conversation  Details for accountNumber : " + accountNumber
					+ " and nickNameID : " + nickNameID + " and queryParam is " + queryParam + " and response is: ", e);
			payload.put("status", "failure");
		}
		exchange.getOut().setBody(payload);
	}

}