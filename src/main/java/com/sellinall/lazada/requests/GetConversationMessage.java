package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.APIUrlConfig;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetConversationMessage implements Processor {
	static Logger log = Logger.getLogger(GetConversationMessage.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accessToken = (String) exchange.getProperty("accessToken");
		String accountNumber = (String) exchange.getProperty("accountNumber");
		String nickNameID = (String) exchange.getProperty("nickNameID");
		String url = (String) exchange.getProperty("hostURL");
		long currentTime = System.currentTimeMillis();
		String lastMsgId = exchange.getProperty("lastMessageID") != null
				? (String) exchange.getProperty("lastMessageID")
				: "";
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("session_id", exchange.getProperty("conversationID", String.class));
		map.put("page_size", exchange.getProperty("pageSize", String.class));

		String queryParam = "&session_id=" + exchange.getProperty("conversationID", String.class);

		if (!lastMsgId.isEmpty()) {
			queryParam += "&last_message_id=" + lastMsgId;
			map.put("last_message_id", lastMsgId);
			currentTime = exchange.getProperty("nextTimeStamp", Long.class);
		}
		map.put("start_time", String.valueOf(currentTime));
		queryParam += "&page_size=" + exchange.getProperty("pageSize", String.class) + "&start_time=" + currentTime;
		String response = NewLazadaConnectionUtil.callAPI(url, "/im/message/list", accessToken, map, "", queryParam,
				"GET", Config.getConfig().getLazadaChatClientID(), Config.getConfig().getLazadaChatClientSecret());
		JSONObject result = new JSONObject(response);
		String status = "failure";

		String errorMessage = "";
		try {
			if (result.has("data")) {
				JSONObject payload = result.getJSONObject("data");
				if (payload.has("error") && !payload.getString("error").isEmpty()) {
					errorMessage = payload.getString("error");
					log.error("Error occurred while getting msg for accountNumber : " + accountNumber
							+ " and nickNameID : " + nickNameID + " and response is " + result.toString());
				} else {
					status = "success";
					exchange.setProperty("response", payload);
				}
			} else {
				errorMessage = "Lazada server not responding please contact to support team";
				log.error("lazada server not responding getting msg for accountNumber : " + accountNumber
						+ " and nickNameID : " + nickNameID + " and response is " + result.toString());
			}
		} catch (Exception e) {
			errorMessage = "internal error please contact to support team";
			log.error("Internal error occurred while getting conversation  msg for accountNumber : " + accountNumber
					+ " and nickNameID : " + nickNameID + " and response is: ", e);
		}
		exchange.setProperty("status", status);
		if (!errorMessage.isEmpty()) {
			exchange.setProperty("errorMessage", errorMessage);
		}
	}

}
