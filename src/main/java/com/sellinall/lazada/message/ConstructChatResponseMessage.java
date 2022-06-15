package com.sellinall.lazada.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class ConstructChatResponseMessage implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject response = new JSONObject();
		if (exchange.getProperties().containsKey("errorMessage")) {
			response.put("status", exchange.getProperty("status", String.class));
			response.put("errorMessage", exchange.getProperty("errorMessage", String.class));
		} else if (exchange.getProperties().containsKey("failureReason")) {
			response.put("status", "failure");
			response.put("errorMessage", exchange.getProperty("failureReason", String.class));
		} else if (exchange.getProperties().containsKey("response")) {
			response.put("status", exchange.getProperty("status", String.class));
			response.put("response", exchange.getProperty("response", JSONObject.class));
		}
		exchange.getOut().setBody(response);
	}

}