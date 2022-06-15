package com.sellinall.lazada.response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.common.LazadaValues;

public class ProcessSubmitListingResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessSubmitListingResponse.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject response = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("isStockUpdateSuccess", false);
		if ((response.has("code") && !response.getString("code").equals("0"))
				|| (response.has("status") && response.getString("status").equals("failure"))) {
			exchange.setProperty("status", "F");
			String failureMessage = "";
			try {
				if (response.has("detail")) {
					JSONArray errors = response.getJSONArray("detail");
					log.debug(errors);
					for (int i = 0; i < errors.length(); i++) {
						JSONObject error = errors.getJSONObject(i);
						failureMessage += error.getString("message");
					}
				} else if (response.has("status") && response.getString("status").equals("failure")) {
					failureMessage += "Gateway Timeout Error.Please retry again!";
				} else if (response.getString("code").equals("IllegalAccessToken")) {
					failureMessage += response.getString("message");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (failureMessage.isEmpty()) {
				exchange.setProperty("updateFailureReason", LazadaValues.SC_SERVER_DOWN.toString());
			} else {
				exchange.setProperty("failureReason", failureMessage);
			}
		} else {
			if (exchange.getProperty("requestType").equals("quantityChange") && response.has("detail")) {
				JSONArray details = response.getJSONArray("detail");
				for (int i = 0; i < details.length(); i++) {
					JSONObject detail = details.getJSONObject(i);
					if (detail.has("message") && detail.getString("message").equals("ITEM_NOT_FOUND")) {
						exchange.setProperty("status", "F");
						exchange.setProperty("failureReason", "Item not found in Lazada Seller Center");
						break;
					}
				}
				return;
			}
			exchange.setProperty("status", "A");
			exchange.setProperty("isStockUpdateSuccess", true);
			if (exchange.getProperties().containsKey("isSalePriceUpdate")
					&& exchange.getProperty("isSalePriceUpdate", Boolean.class)) {
				exchange.setProperty("isPromotionEnabled", true);
			} else if (exchange.getProperties().containsKey("isSalePriceRemove")
					&& exchange.getProperty("isSalePriceRemove", Boolean.class)) {
				exchange.setProperty("isPromotionRemoved", true);
			}
		}
	}
}