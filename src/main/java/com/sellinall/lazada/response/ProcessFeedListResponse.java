package com.sellinall.lazada.response;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ProcessFeedListResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessFeedListResponse.class.getName());

	public void process(Exchange exchange) throws Exception {

		String response = exchange.getIn().getBody(String.class);
		ArrayList<JSONObject> arrayList = new ArrayList<JSONObject>();
		exchange.setProperty("isResponseHasFeed", false);
		try {
			JSONObject serviceResponse = new JSONObject(response);
			if (!serviceResponse.has("SuccessResponse")) {
				return;
			}
			JSONObject successResponse = serviceResponse.getJSONObject("SuccessResponse");
			JSONObject body = successResponse.getJSONObject("Body");
			if (body.get("Feed") instanceof String) {
				return;
			}
			Object feeds = body.get("Feed");
			if (feeds instanceof JSONObject) {
				arrayList.add((JSONObject)feeds);
			} else if (feeds instanceof JSONArray) {
				JSONArray feedList = (JSONArray) feeds;
				for (int i = 0; i < feedList.length(); i++) {
					JSONObject channelItem = feedList.getJSONObject(i);
					arrayList.add(channelItem);
				}
			}
			exchange.setProperty("isResponseHasFeed", true);
		} catch (Exception e) {
			e.printStackTrace();
			log.debug("No Feed found");
		}
		exchange.getOut().setBody(arrayList);
	}
}