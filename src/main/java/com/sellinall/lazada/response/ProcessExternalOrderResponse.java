package com.sellinall.lazada.response;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ProcessExternalOrderResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessExternalOrderResponse.class.getName());

	public void process(Exchange exchange) throws Exception {
		System.out.println(exchange.getProperty("ordersList"));
		JSONObject response = new JSONObject();
		ArrayList<JSONObject> orderList = exchange.getProperty("ordersList", ArrayList.class);
		JSONArray orders = new JSONArray();
		for(JSONObject order : orderList) {
			orders.put(order);
		}
		response.put("orders", orders);
		exchange.getOut().setBody(response);
	}
}