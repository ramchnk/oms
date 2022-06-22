package com.sellinall.lazada.response;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;

public class ProcessListOrdersResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessListOrdersResponse.class.getName());

	public void process(Exchange exchange) throws Exception {

		String response = exchange.getIn().getBody(String.class);
		ArrayList<JSONObject> arrayList = exchange.getProperty("ordersList", ArrayList.class);
		exchange.setProperty("isResponseHasOrder", false);
		try {
			JSONObject serviceResponse = new JSONObject(response);
			JSONObject body = new JSONObject();
			JSONArray orders = new JSONArray();
			if (!serviceResponse.getString("code").equals("0")) {
				return;
			}
			String accountNumber = exchange.getProperty("accountNumber", String.class);
			String nickNameID = exchange.getProperty("nickNameID", String.class);
			body = serviceResponse.getJSONObject("data");
			if (!body.has("orders")) {
				exchange.setProperty("isOrderPollingScuccess", false);
				log.error("Getting invalid response from order api for accountNumber : " + accountNumber
						+ ", nickNameID : " + nickNameID + " & response : " + response);
				return;
			}
			if (body.get("orders") instanceof String) {
				exchange.setProperty("isOrderPollingScuccess", false);
				log.error("Getting string response from order polling api for accountNumber : " + accountNumber
						+ " nickNameID : " + nickNameID + ", response : " + serviceResponse);
				return;
			}
			if (body.has("count") && body.getInt("count") > 0) {
				int pagingLimit = Integer.parseInt(Config.getConfig().getOrderLimit());
				int paidOrdersCount = exchange.getProperty("paidOrdersCount", Integer.class);

				boolean callPaidOrdersAPI = (paidOrdersCount == pagingLimit) ? true : false;

				if (callPaidOrdersAPI) {
					int offSet = Integer.parseInt(exchange.getProperty("pageOffSet", String.class)) + pagingLimit;
					exchange.setProperty("pageOffSet", offSet);
					exchange.setProperty("hasMoreRecords", true);
				}
				exchange.setProperty("callPaidOrdersAPI", callPaidOrdersAPI);
				orders = body.getJSONArray("orders");
				for (int i = 0; i < orders.length(); i++) {
					JSONObject channelItem = orders.getJSONObject(i);
					arrayList.add(channelItem);
				}
				if (arrayList.size() > 0) {
					exchange.setProperty("isResponseHasOrder", true);
				} else {
					exchange.setProperty("isResponseHasOrder", false);
				}
			}
		} catch (Exception e) {
			exchange.setProperty("isOrderPollingScuccess", false);
			log.error("Exception occured while processing order for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response : " + response);
			e.printStackTrace();
		}
		log.debug(exchange.getProperty("isResponseHasOrder"));
	}

}