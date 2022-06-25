package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class InitGetOrderDetails implements Processor {

	static Logger log = Logger.getLogger(InitGetOrderDetails.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("orderID", inBody.getString("orderID"));
	}

}
