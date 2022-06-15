package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;


public class InitializeProcessPullInventoryRoute implements Processor {

	public void process(Exchange exchange) throws Exception {

		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		if(!inBody.has("retryCount")) {
			inBody.put("retryCount", 0);
		}
		exchange.setProperty("currentPageMessage", inBody);
		String nickNameID = inBody.getString("nickNameID");
		exchange.setProperty("isInventoryEmpty", inBody.getBoolean("isInventoryEmpty"));
		if (inBody.has("numberOfRecords")) {
			exchange.setProperty("numberOfRecords", inBody.getInt("numberOfRecords"));
		}
		if (inBody.has("importStatusFilter")) {
			exchange.setProperty("importStatusFilter", inBody.getString("importStatusFilter"));
		}
		exchange.setProperty("noOfItemCompleted", inBody.getInt("noOfItemCompleted"));
		exchange.setProperty("noOfItemLinked", inBody.getInt("noOfItemLinked"));
		exchange.setProperty("noOfItemUnLinked", inBody.getInt("noOfItemUnLinked"));
		exchange.setProperty("noOfItemSkipped", inBody.getInt("noOfItemSkipped"));
		exchange.setProperty("accountNumber", inBody.getString("accountNumber"));
		exchange.setProperty("importRecordObjectId", inBody.getString("importRecordObjectId"));
		exchange.setProperty("nickNameID", nickNameID);
		exchange.setProperty("pageNumber", inBody.getInt("pageNumber"));
		exchange.setProperty("totalEntries", inBody.getInt("totalEntries"));
		if (inBody.has("numberOfPages")) {
			exchange.setProperty("numberOfPages", inBody.getInt("numberOfPages"));
		}
		exchange.setProperty("offset", inBody.getInt("offset"));
		exchange.setProperty("recordsPerPage", Config.getConfig().getRecordsPerPage());
		exchange.setProperty("channelName", nickNameID.split("-")[0]);
		exchange.setProperty("isLastLoop", false);
		if (inBody.has("userId")) {
			exchange.setProperty("userId", inBody.getString("userId"));
		}
		if (inBody.has("userName")) {
			exchange.setProperty("userName", inBody.getString("userName"));
		}
		if (inBody.has("totalProducts")) {
			exchange.setProperty("totalProducts", inBody.getInt("totalProducts"));
		}
		if (inBody.has("totalChildItemEntries")) {
			exchange.setProperty("totalChildItemEntries", inBody.getInt("totalChildItemEntries"));
		}
		if (inBody.has("importType")) {
			exchange.setProperty("importType", inBody.getString("importType"));
		}
		if (inBody.has("fromDate")) {
			exchange.setProperty("fromDate", inBody.getInt("fromDate"));
		}
		if (inBody.has("toDate")) {
			exchange.setProperty("toDate", inBody.getInt("toDate"));
		}
		if(inBody.has("countryCode")) {
			exchange.setProperty("importCountry", inBody.getString("countryCode"));
		}
	}
}
