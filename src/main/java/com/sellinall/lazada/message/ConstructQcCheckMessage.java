package com.sellinall.lazada.message;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class ConstructQcCheckMessage implements Processor {
	static Logger log = Logger.getLogger(ConstructQcCheckMessage.class.getName());

	public void process(Exchange exchange) throws Exception {
		ArrayList<String> inBody = exchange.getIn().getBody(ArrayList.class);
		JSONObject payload = new JSONObject();
		payload.put("accountNumber", exchange.getProperty("accountNumber"));
		payload.put("nickNameID", exchange.getProperty("nickNameID"));
		payload.put("skuAndRefrenceIdList", inBody);
		payload.put("requestType", "checkQcStatus");
		exchange.getOut().setBody(payload);
	}
}
