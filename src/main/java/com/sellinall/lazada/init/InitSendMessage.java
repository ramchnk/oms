package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class InitSendMessage implements Processor {
	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("accountNumber", inBody.getString("accountNumber"));
		exchange.setProperty("nickNameID", inBody.getString("nickNameID"));
		exchange.setProperty("appType", "chat");
		exchange.setProperty("toId", inBody.getString("toId"));
		exchange.setProperty("messageType", inBody.getString("messageType"));
		exchange.setProperty("content", inBody.getJSONObject("content"));
		exchange.setProperty("conversationID", inBody.getString("conversationID"));
		if (inBody.has("lastReadMessageID")) {
			exchange.setProperty("lastReadMessageID", inBody.getString("lastReadMessageID"));
		}
		if (inBody.getString("messageType").equals("image")) {
			exchange.setProperty("isImageUpload", true);
		}
	}

}
