package com.sellinall.lazada.message;

import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class ConstructMessageForBatch implements Processor {
	static Logger log = Logger.getLogger(ConstructMessageForBatch.class.getName());

	public void process(Exchange exchange) throws Exception {
		log.debug("Inside ConstructMessageForBatch");

		int noOfItemCompleted = (Integer) exchange.getProperty("noOfItemCompleted");
		int noOfItemSkipped = (Integer) exchange.getProperty("noOfItemSkipped");
		JSONObject message = new JSONObject();
		if (exchange.getIn().getHeaders().containsKey("importStatus")) {
			String status = exchange.getIn().getHeader("importStatus", String.class);
			message.put("status", status);
		}
		if (exchange.getIn().getHeaders().containsKey("exceptionMessage")) {
			log.error("Error occured on pull inventory route for account Number : "
					+ exchange.getProperty("accountNumber") + " importRecordObjectId:  "
					+ exchange.getProperty("importRecordObjectId") + " and error: "
					+ exchange.getIn().getHeader("exceptionMessage"));
		}
		if (exchange.getProperties().containsKey("failureReason")){
			message.put("failureReason", exchange.getProperty("failureReason", String.class));
		}
		if (exchange.getProperties().containsKey("totalEntries") && exchange.getProperties().containsKey("importType")
				&& exchange.getProperty("importType", String.class).equals("dateRange")) {
			message.put("noOfRecords", exchange.getProperty("totalEntries", Integer.class));
		} else {
			message.put("noOfRecords", exchange.getProperty("numberOfRecords", Integer.class));
		}
		message.put("skipped", noOfItemSkipped);
		message.put("imported", noOfItemCompleted);
		message.put("linked", exchange.getProperty("noOfItemLinked", Integer.class));
		message.put("unLinked", exchange.getProperty("noOfItemUnLinked", Integer.class));
		message.put("accountNumber", exchange.getProperty("accountNumber"));
		message.put("importRecordObjectId", exchange.getProperty("importRecordObjectId"));
		message.put("requestType", "importInventoryUpdate");
		if (exchange.getProperties().containsKey("itemIDList")) {
			message.put("inventoryList", exchange.getProperty("itemIDList", Set.class));
		}
		log.debug(message.toString());
		exchange.getOut().setBody(message);
	}

}
