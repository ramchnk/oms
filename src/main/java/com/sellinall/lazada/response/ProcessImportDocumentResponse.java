package com.sellinall.lazada.response;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

public class ProcessImportDocumentResponse implements Processor {

	public void process(Exchange exchange) throws Exception {
		BasicDBObject response = (BasicDBObject) JSON.parse(exchange.getIn().getBody(String.class));
		List<String> pullInventoryList = (List<String>) response.get("inventoryList");
		Set<String> itemIDList = new HashSet<String>(pullInventoryList);
		exchange.setProperty("itemIDList", itemIDList);
	}

}
