package com.sellinall.lazada.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class ConstructPagingMessage implements Processor{

	public void process(Exchange exchange) throws Exception {

		exchange.setProperty("lazadaRoutingKey", "lazadaListingKey");
		JSONObject message = new JSONObject();
		if (exchange.getProperties().containsKey("isRetry")
				&& exchange.getProperty("isRetry", boolean.class)) {
			if (exchange.getProperties().containsKey("currentPageMessage")) {
				// Retrying service time-out crossed after first page.
				message = exchange.getProperty("currentPageMessage", JSONObject.class);
			} else {
				// First page service time-out handled here.
				message = exchange.getProperty("initialRetryMessage", JSONObject.class);
			}
			if (exchange.getProperty("retryLimitExceeded", boolean.class)) {
				exchange.setProperty("lazadaRoutingKey", "lazada60SecDelayKey");
				message.remove("retryCount");
			}
		} else {
			message.put("isInventoryEmpty", exchange.getProperty("isInventoryEmpty", Boolean.class));
			message.put("numberOfRecords", exchange.getProperty("numberOfRecords", Integer.class));
			if(exchange.getProperties().containsKey("importStatusFilter")) {
				message.put("importStatusFilter", exchange.getProperty("importStatusFilter", String.class));
			}
			message.put("noOfItemCompleted", exchange.getProperty("noOfItemCompleted"));
			message.put("noOfItemLinked", exchange.getProperty("noOfItemLinked"));
			message.put("noOfItemUnLinked", exchange.getProperty("noOfItemUnLinked"));
			message.put("noOfItemSkipped", exchange.getProperty("noOfItemSkipped"));
			message.put("accountNumber", exchange.getProperty("accountNumber"));
			message.put("importRecordObjectId", exchange.getProperty("importRecordObjectId"));
			message.put("pageNumber", exchange.getProperty("pageNumber", Integer.class));
			message.put("numberOfPages", exchange.getProperty("numberOfPages", Integer.class));
			message.put("nickNameID", exchange.getProperty("nickNameID", String.class));
			message.put("requestType", "processPullInventoryByPage");
			message.put("totalEntries", exchange.getProperty("totalEntries", Integer.class));
			message.put("offset", exchange.getProperty("offset", Integer.class));
			if(exchange.getProperty("userId") != null) {
				message.put("userId", exchange.getProperty("userId", String.class));
			}
			if(exchange.getProperty("userName") != null) {
				message.put("userName", exchange.getProperty("userName", String.class));
			}
			if(exchange.getProperties().containsKey("totalProducts")) {
				message.put("totalProducts", exchange.getProperty("totalProducts", Integer.class));
			}
			if(exchange.getProperties().containsKey("totalChildItemEntries")) {
				message.put("totalChildItemEntries", exchange.getProperty("totalChildItemEntries", Integer.class));
			}
			if (exchange.getProperties().containsKey("importType")) {
				message.put("importType", exchange.getProperty("importType", String.class));
			}
			if (exchange.getProperties().containsKey("fromDate")) {
				message.put("fromDate", exchange.getProperty("fromDate", String.class));
			}
			if (exchange.getProperties().containsKey("toDate")) {
				message.put("toDate", exchange.getProperty("toDate", String.class));
			}
			if (exchange.getProperties().containsKey("importCountry")) {
				message.put("countryCode", exchange.getProperty("importCountry", String.class));
			}
		}
		exchange.getOut().setBody(message);
	}

}
