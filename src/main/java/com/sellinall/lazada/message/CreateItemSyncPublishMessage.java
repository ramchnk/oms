package com.sellinall.lazada.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class CreateItemSyncPublishMessage implements Processor {
	static Logger log = Logger.getLogger(CreateItemSyncPublishMessage.class.getName());

	public void process(Exchange exchange) throws Exception {
		if(exchange.getIn().getHeaders().containsKey("exceptionMessage")){
			log.error(exchange.getIn().getHeader("exceptionMessage"));
		}
		String accountNumber = (String) exchange.getProperty("accountNumber");
		String requestType = exchange.getProperty("requestType", String.class);
		JSONObject json = new JSONObject();
		json.put("accountNumber", accountNumber);
		json.put("channel", "lazada");
		json.put("requestType", requestType);
		log.debug("Item sync Message : " + json.toString());
		exchange.getOut().setBody(json);
		log.debug("created item sync message");
		long endTime = System.currentTimeMillis();
		if (exchange.getProperties().containsKey("StartTime")) {
			log.info("requestType : " + requestType + " - processing time : "
					+ ((endTime - exchange.getProperty("StartTime", Long.class)) / 1000) + " seconds"
					+ " for this accountNumber : " + accountNumber);
		}
	}
}
