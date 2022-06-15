package com.sellinall.lazada.response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ProcessFeedDetailResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessFeedDetailResponse.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject feedDetail = exchange.getIn().getBody(JSONObject.class);
		log.debug("Feed Response Details "+feedDetail.toString());
		exchange.setProperty("isFeedStatusCompleted", false);
		exchange.setProperty("isFeedActionSuccess", false);
		try {
			if (feedDetail.getString("Status").equals("Finished")) {
				exchange.setProperty("isFeedStatusCompleted", true);
				if (!feedDetail.has("FeedErrors")) {
					exchange.setProperty("isFeedActionSuccess", true);
					return;
				}
				if(feedDetail.get("FeedErrors") instanceof String){
					log.debug("Feed Response Details "+feedDetail.toString());
					exchange.setProperty("isFeedActionSuccess", true);
					return;
				}

				JSONObject feedErrors = feedDetail.getJSONObject("FeedErrors");
				if (feedErrors.get("Error") instanceof JSONObject) {
					JSONObject error = feedErrors.getJSONObject("Error");
					exchange.setProperty("failureReason", error.getString("Message"));
					exchange.setProperty("sellerSKU", error.getString("SellerSku"));
				} else if (feedErrors.get("Error") instanceof JSONArray) {
					JSONArray errors = feedErrors.getJSONArray("Error");
					String errorMsg = "";
					String sellerSKU = "";
					for (int i = 0; i < errors.length(); i++) {
						JSONObject error = errors.getJSONObject(i);
						errorMsg = errorMsg + "#" + (i + 1) + " ) " + error.getString("Message") + "; ";
						sellerSKU = error.getString("SellerSku");
					}
					exchange.setProperty("sellerSKU", sellerSKU);
					exchange.setProperty("failureReason", errorMsg);
				}
			}

			exchange.setProperty("isValidFeed", true);
		} catch (Exception e) {
			log.error("Feed unavailable ");
		}
	}
}