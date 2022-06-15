package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class InitUpdateSessionAsRead implements Processor {
	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("accountNumber", inBody.getString("accountNumber"));
		exchange.setProperty("nickNameID", inBody.getString("nickNameID"));
		exchange.setProperty("sessionID", inBody.getString("conversationID"));
		exchange.setProperty("lastReadMessageId", inBody.getString("lastReadMessageId"));
		exchange.setProperty("appType", "chat");
	}

}
