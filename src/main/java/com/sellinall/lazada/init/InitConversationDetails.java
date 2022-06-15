package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class InitConversationDetails implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		String accountNumber = inBody.getString("accountNumber");
		String nickNameID = inBody.getString("nickNameID");
		String conversationID = inBody.getString("conversationID");

		exchange.setProperty("accountNumber", accountNumber);
		exchange.setProperty("nickNameID", nickNameID);
		exchange.setProperty("conversationID", conversationID);
		exchange.setProperty("appType", "chat");
	}

}
