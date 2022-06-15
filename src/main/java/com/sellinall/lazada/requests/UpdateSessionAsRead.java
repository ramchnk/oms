package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class UpdateSessionAsRead implements Processor {

	static Logger log = Logger.getLogger(UpdateSessionAsRead.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		String accountNumber = (String) exchange.getProperty("accountNumber");
		String nickNameID = (String) exchange.getProperty("nickNameID");
		String url = exchange.getProperty("hostURL", String.class);
		String accessToken = (String) exchange.getProperty("accessToken");
		String sessionID = exchange.getProperty("sessionID", String.class);
		String lastReadMessageID = exchange.getProperty("lastReadMessageId", String.class);

		String clientID = Config.getConfig().getLazadaChatClientID();
		String clientSecret = Config.getConfig().getLazadaChatClientSecret();

		String param = "&session_id=" + sessionID + "&last_read_message_id=" + lastReadMessageID;

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("session_id", sessionID);
		map.put("last_read_message_id", lastReadMessageID);

		String response = "";
		try {
			response = NewLazadaConnectionUtil.callAPI(url, "/im/session/read", accessToken, map, "", param, "POST",
					clientID, clientSecret);
			if (!response.isEmpty() && response.startsWith("{")) {
				JSONObject responseObj = new JSONObject(response);
				if (responseObj.getString("err_code").equals("0")) {
					log.info("Successfully updated session as read for accountNumber : " + accountNumber
							+ ", nickNameID : " + nickNameID + ", sessionID : " + sessionID + " & param : " + param
							+ " & response : " + response);
				} else {
					log.error("Got failure response while updating session as read for accountNumber : " + accountNumber
							+ ", nickNameID : " + nickNameID + ", sessionID : " + sessionID + " & param : " + param
							+ " & response : " + response);
				}
			} else {
				log.error("Invalid response while updating session as read for accountNumber : " + accountNumber
						+ ", nickNameID : " + nickNameID + ", sessionID : " + sessionID + " & param : " + param
						+ " & response : " + response);
			}
		} catch (Exception e) {
			log.error("Exception occured while updating session as read for accountNumber : " + accountNumber
					+ ", nickNameID : " + nickNameID + " & sessionID : " + sessionID + " & param : " + param
					+ " & response : " + response);
			e.printStackTrace();
		}
	}

}
