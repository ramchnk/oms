package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.APIUrlConfig;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetConversationList implements Processor {
	static Logger log = Logger.getLogger(GetConversationList.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accessToken = (String) exchange.getProperty("accessToken");
		String accountNumber = (String) exchange.getProperty("accountNumber");
		String nickNameID = (String) exchange.getProperty("nickNameID");
		String url = (String) exchange.getProperty("hostURL");
		String lastSessionId = exchange.getProperty("lastSessionID") != null
				? (String) exchange.getProperty("lastSessionID")
				: "";
		long currentTime = System.currentTimeMillis();
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("page_size", exchange.getProperty("pageSize", String.class));

		String queryParam = "&page_size=" + exchange.getProperty("pageSize", String.class);
		if (!lastSessionId.isEmpty()) {
			map.put("last_session_id", lastSessionId);
			queryParam += "&last_session_id=" + lastSessionId;
			currentTime = exchange.getProperty("nextTimeStamp", Long.class);
		}
		map.put("start_time", String.valueOf(currentTime));
		queryParam += "&start_time=" + currentTime;

		String result = NewLazadaConnectionUtil.callAPI(url, "/im/session/list", accessToken, map, "", queryParam,
				"GET", Config.getConfig().getLazadaChatClientID(), Config.getConfig().getLazadaChatClientSecret());
		JSONObject response = new JSONObject(result);
		String status = "failure";
		String errorMessage = "";
		try {
			if (response.has("data")) {
				JSONObject payload = response.getJSONObject("data");
				if (payload.has("error") && !payload.getString("error").isEmpty()) {
					errorMessage = payload.getString("error");
					log.error("Error occurred while getting msg for accountNumber : " + accountNumber
							+ " and nickNameID : " + nickNameID + " and queryParam is " + queryParam
							+ " and response is " + response.toString());
				} else {
					status = "success";
					exchange.setProperty("response", payload);
				}
			} else {
				errorMessage = "Lazada server not responding please contact to support team";
				log.error("Lazada server not responding while getting msg for accountNumber : " + accountNumber
						+ " and nickNameID : " + nickNameID + " and queryParam is " + queryParam + " and response is "
						+ response.toString());
			}
		} catch (Exception e) {
			errorMessage = "internal error please contact to support team";
			log.error("Internal error occurred while getting conversation list for accountNumber : " + accountNumber
					+ " and nickNameID : " + nickNameID + " and queryParam is " + queryParam + " and response is: ", e);
		}
		exchange.setProperty("status", status);
		if (!errorMessage.isEmpty()) {
			exchange.setProperty("errorMessage", errorMessage);
		}
	}

}
