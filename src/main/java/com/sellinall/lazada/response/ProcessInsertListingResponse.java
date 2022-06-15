package com.sellinall.lazada.response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;

public class ProcessInsertListingResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessInsertListingResponse.class.getName());

	public void process(Exchange exchange) throws Exception {
		String response = exchange.getIn().getBody(String.class);
		int statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		if (statusCode == HttpStatus.OK_200) {
			if (!exchange.getProperties().containsKey("isImportMissingItem")) {
				int noOfItemLinked = exchange.getProperty("noOfItemLinked", Integer.class);
				exchange.setProperty("noOfItemLinked", noOfItemLinked + 1);
			}
		} else {
			log.error("Error occured while inserting inventory for accountNumber: " + accountNumber + ", nickNameID: "
					+ nickNameID + " and the response is: " + response);
			if (exchange.getProperties().containsKey("isImportMissingItem")) {
				exchange.setProperty("isImportMissingItem", false);
			} else {
				int noOfItemSkipped = exchange.getProperty("noOfItemSkipped", Integer.class);
				exchange.setProperty("noOfItemSkipped", noOfItemSkipped + 1);
			}
		}
	}
}
