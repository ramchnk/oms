package com.sellinall.lazada.response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;


public class ProcessGetOrdersResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessGetOrdersResponse.class.getName());
	public void process(Exchange exchange) throws Exception {
		String response = exchange.getIn().getBody(String.class);
		if (!response.startsWith("{")) {
			JSONObject serviceResponse = new JSONObject();
			serviceResponse.put("status", "failure");
			serviceResponse.put("failureReason", "Unable to get details and response is :" + response);
			exchange.getOut().setBody(serviceResponse);
			return;
		}
		JSONObject serviceResponse = new JSONObject(response);
		try {
			JSONObject body = new JSONObject();
			if (!serviceResponse.getString("code").equals("0")) {
				serviceResponse.put("status", "failure");
				exchange.getOut().setBody(serviceResponse);
				return;
			}
			String accountNumber = exchange.getProperty("accountNumber", String.class);
			String nickNameID = exchange.getProperty("nickNameID", String.class);
			body = serviceResponse.getJSONObject("data");
			if (!body.has("orders")) {
				serviceResponse.put("status", "failure");
				exchange.getOut().setBody(serviceResponse);
				log.error("Getting invalid response from order api for accountNumber : " + accountNumber
						+ ", nickNameID : " + nickNameID + " & response : " + response);
				return;
			}
			if (body.get("orders") instanceof String) {
				serviceResponse.put("status", "failure");
				exchange.getOut().setBody(serviceResponse);
				log.error("Getting string response from order polling api for accountNumber : " + accountNumber
						+ " nickNameID : " + nickNameID + ", response : " + serviceResponse);
				return;
			}
			int paidOrdersCount = 0;
			int unpaidOrdersCount = 0;
			if (exchange.getProperties().containsKey("paidOrdersCount")) {
				paidOrdersCount = exchange.getProperty("paidOrdersCount", Integer.class);
			}
			if (exchange.getProperties().containsKey("unpaidOrdersCount")) {
				unpaidOrdersCount = exchange.getProperty("unpaidOrdersCount", Integer.class);
			}
			int limit = Integer.parseInt(Config.getConfig().getOrderLimit());
			if (paidOrdersCount == limit || unpaidOrdersCount == limit) {
				serviceResponse.put("hasMoreRecords", true);
			}
		} catch (Exception e) {
			log.error("Exception occured while processing order for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response : " + response);
			e.printStackTrace();
		}
		exchange.getOut().setBody(serviceResponse);
	}
}
