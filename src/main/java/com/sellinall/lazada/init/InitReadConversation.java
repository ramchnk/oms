package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class InitReadConversation implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("accountNumber", inBody.getString("accountNumber"));
		exchange.setProperty("nickNameID", inBody.getString("nickNameID"));
		exchange.setProperty("appType", "chat");
		if (inBody.has("pageSize")) {
			exchange.setProperty("pageSize", inBody.getInt("pageSize"));
		}
		if (inBody.has("nextTimeStamp")) {
			Long nextTimeStamp = inBody.getLong("nextTimeStamp");
			if (nextTimeStamp > 0) {
				exchange.setProperty("nextTimeStamp", nextTimeStamp);
			}
		}
		if (inBody.has("lastSessionID")) {
			exchange.setProperty("lastSessionID", inBody.getString("lastSessionID"));
		}
		if (inBody.has("lastMessageID")) {
			exchange.setProperty("lastMessageID", inBody.getString("lastMessageID"));
		}
		if (inBody.has("conversationID")) {
			exchange.setProperty("conversationID", inBody.getString("conversationID"));
		}
	}

}
