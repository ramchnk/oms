package com.sellinall.lazada.message;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class PrepareBatchMessageForStatusUpdate implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject inputRequest = exchange.getProperty("inputRequest", JSONObject.class);
		List<String> failureReasons = exchange.getProperty("failureReasons", List.class);
		JSONObject message = new JSONObject();
		message.put("sellerSKU", exchange.getProperty("sellerSKU", String.class));
		message.put("rowIdentifier", inputRequest.getJSONObject("rowIdentifier"));
		if (exchange.getProperties().containsKey("skuListToProcess")
				&& exchange.getProperty("skuListToProcess", List.class).size() == 0) {
			message.put("status", "failure");
			message.put("failureReason", "customSKU not found");
		} else if (failureReasons.size() == 0) {
			message.put("status", "success");
		} else {
			message.put("status", "failure");
			message.put("failureReason", constructFailureMessage(failureReasons));
		}
		exchange.getOut().setBody(message);
	}

	private String constructFailureMessage(List<String> failureReasons) {
		String message = "Failed to update status for ";
		for (int i = 0; i < failureReasons.size(); i++) {
			if (i == failureReasons.size() - 1) {
				message += failureReasons.get(i);
			} else {
				message += failureReasons.get(i) + ", ";
			}
		}
		return message;
	}

}
