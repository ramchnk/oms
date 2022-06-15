package com.sellinall.lazada.response;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;

public class ProcessPullResult implements Processor {
	static Logger log = Logger.getLogger(ProcessPullResult.class.getName());

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void process(Exchange exchange) throws Exception {
		Object unlinkedInventoryDocs = exchange.getIn().getBody();
		if (unlinkedInventoryDocs instanceof List && ((List) unlinkedInventoryDocs).size() > 0) {
			exchange.getOut().setHeader("hasRecordsToInsert", true);
			exchange.setProperty("unlinkedInventoryDocs", unlinkedInventoryDocs);
			exchange.getOut().setBody(unlinkedInventoryDocs);
			int recordsProcessed = exchange.getProperty("recordsProcessed", Integer.class);
			recordsProcessed += ((List<BasicDBObject>) unlinkedInventoryDocs).size();
			exchange.setProperty("recordsProcessed", recordsProcessed);
			return;
		}
		exchange.getOut().setHeader("hasRecordsToInsert", false);
	}
}