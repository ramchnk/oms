package com.sellinall.lazada.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class CreatePublishMessageToFB implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject outBody = new JSONObject();
		outBody.put("SKU", exchange.getProperty("linkToSKU", String.class));
		outBody.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		outBody.put("requestType", "addItem");
		outBody.put("requestChannelID", exchange.getProperty("nickNameID", String.class));
		exchange.getOut().setBody(outBody);
	}

}