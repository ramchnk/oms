package com.sellinall.lazada.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class CreateListingFeedPollingMessage implements Processor {
	static Logger log = Logger.getLogger(CreateListingFeedPollingMessage.class.getName());

	public void process(Exchange exchange) throws Exception {

		String accountNumber = (String) exchange.getProperty("accountNumber");
		String requestType = exchange.getProperty("requestType", String.class);
		JSONObject json = new JSONObject();
		json.put("accountNumber", accountNumber);
		json.put("requestType", requestType);
		exchange.getOut().setBody(json);
	}
}
