package com.sellinall.lazada.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class ConstructResponseMessage implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject response = new JSONObject();
		response.put("response", "success");
		response.put("responseMessage", "success");
		if (exchange.getProperties().containsKey("merchantID")) {
			response.put("merchantID", exchange.getProperty("merchantID", String.class));
		}
		if (exchange.getProperties().containsKey("failureReason")
				&& !exchange.getProperty("failureReason", String.class).isEmpty()) {
			response.put("response", "failure");
			response.put("responseMessage", exchange.getProperty("failureReason", String.class));
		}
		exchange.getOut().setBody(response);
	}

}