package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.APIUrlConfig;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class RecallConversation implements Processor {
	static Logger log = Logger.getLogger(RecallConversation.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accessToken = exchange.getProperty("accessToken", String.class);
		String countryCode = exchange.getProperty("countryCode", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String messageID = exchange.getProperty("messageID", String.class);
		String sessionID = exchange.getProperty("sessionID", String.class);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("session_id", sessionID);
		headers.put("message_id", messageID);
		headers.put("access_token", accessToken);

		String queryParams = "";
		queryParams += "&session_id=" + sessionID;
		queryParams += "&message_id=" + messageID;

		String url = APIUrlConfig.getConfig().getNewAPIUrl(countryCode);
		try {
			String result = NewLazadaConnectionUtil.callAPI(url, "/im/message/recall", accessToken, headers, "",
					queryParams, "POST", Config.getConfig().getLazadaChatClientID(),
					Config.getConfig().getLazadaChatClientSecret());
			log.info("Successfully recalled message for accountNumber : " + accountNumber + " and nickNameID : "
					+ nickNameID + " and queryParams is: " + queryParams + " and response is: " + result);
			JSONObject response = new JSONObject(result);
			exchange.getOut().setBody(response);
		} catch (Exception e) {
			log.error("Internal error occurred while recall message for accountNumber : " + accountNumber
					+ " and nickNameID : " + nickNameID + " and queryParams is: " + queryParams + " and response is: ",
					e);
		}

	}

}