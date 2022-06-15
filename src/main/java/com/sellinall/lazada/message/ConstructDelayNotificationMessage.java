package com.sellinall.lazada.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class ConstructDelayNotificationMessage implements Processor {

	static Logger log = Logger.getLogger(ConstructDelayNotificationMessage.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);

		JSONObject msg = new JSONObject();
		msg.put("accountNumber", accountNumber);
		msg.put("requestType", exchange.getProperty("requestType"));

		log.info("publishing delay notification polling msg for accountNumber : " + accountNumber);
		exchange.getOut().setBody(msg);
	}

}
