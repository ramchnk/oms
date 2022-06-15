package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.util.ListingStockEventSequence;

public class InitializeJournalMessage implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject request = exchange.getProperty("request", JSONObject.class);
		String failureReason = "";
		if (exchange.getProperties().containsKey("isUpdateStockViaProductUpdateApi")
				&& !exchange.getProperty("isUpdateStockViaProductUpdateApi", Boolean.class)
				&& exchange.getProperties().containsKey("isStockUpdateSuccess")
				&& exchange.getProperty("isStockUpdateSuccess", Boolean.class)
				&& exchange.getProperties().containsKey("statusToUpdate")) {
			exchange.removeProperty("updateFailureReason");
			exchange.removeProperty("failureReason");
		}
		if (exchange.getProperties().containsKey("failureReason")) {
			failureReason = exchange.getProperty("failureReason", String.class);
		} else if (exchange.getProperties().containsKey("updateFailureReason")) {
			failureReason = exchange.getProperty("updateFailureReason", String.class);
		}
		String SKU = exchange.getProperty("SKU", String.class);
		String sellerSKU = exchange.getProperty("individualListingSellerSKU", String.class);
		JSONArray eventMessages = new JSONArray();
		JSONObject eventMessage = new JSONObject();
		if (failureReason.isEmpty()) {
			eventMessage.put("status", "success");
		} else {
			eventMessage.put("status", "failure");
			eventMessage.put("error", failureReason);
		}
		JSONObject addendum = new JSONObject();
		eventMessage.put("SKU", SKU);
		eventMessage.put("accountNumber", request.getString("accountNumber"));
		eventMessage.put("sellerSKU", sellerSKU);
		if (request.has("eventID")) {
			eventMessage.put("eventID", request.getString("eventID"));
		}
		eventMessage.put("impactID",
				ListingStockEventSequence.getNextListingStockEvent(exchange.getProperty("merchantID", String.class)));
		eventMessage.put("impactTime", System.currentTimeMillis() / 1000);
		String impact = "Quantity updated";
		if (request.has("isMaxQuantityRemoved") && request.getBoolean("isMaxQuantityRemoved")) {
			addendum.put("isMaxQuantityRemoved", true);
		} else if (exchange.getProperties().containsKey("maxQuantity")) {
			addendum.put("maxQuantity", exchange.getProperty("maxQuantity"));
		}
		if (request.has("isBufferQuantityRemoved") && request.getBoolean("isBufferQuantityRemoved")) {
			addendum.put("isBufferQuantityRemoved", true);
		}
		if (exchange.getProperties().containsKey("bufferQuantity")) {
			if (exchange.getProperties().containsKey("bufferType")) {
				addendum.put("bufferType", exchange.getProperty("bufferType"));
			}
			addendum.put("bufferQuantity", exchange.getProperty("bufferQuantity"));
		}
		if (request.has("userName")) {
			addendum.put("userName", request.getString("userName"));
		}
		if (exchange.getProperties().containsKey("occupiedQuantity")) {
			addendum.put("occupiedQuantity", exchange.getProperty("occupiedQuantity"));
		}
		eventMessage.put("impact", impact);
		eventMessage.put("impactSnapshot", exchange.getProperty("impactSnapshot"));
		addendum.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		if (request.has("addendum") && request.get("addendum") instanceof JSONObject
				&& request.getJSONObject("addendum").has("documentObjectId")) {
			addendum.put("documentObjectId", request.getJSONObject("addendum").getString("documentObjectId"));
		}
		eventMessage.put("addendum", addendum);
		eventMessage.put("adjustedBy", "System");
		if (request.has("stockEventType")) {
			eventMessage.put("stockEventType", request.getString("stockEventType"));
		}
		eventMessages.put(eventMessage);
		exchange.getOut().setBody(eventMessages);

	}

}