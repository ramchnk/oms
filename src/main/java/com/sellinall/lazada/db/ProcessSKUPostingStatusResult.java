/**
 * 
 */
package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

/**
 * @author Senthil
 *
 */
public class ProcessSKUPostingStatusResult implements Processor {

	public void process(Exchange exchange) throws Exception {

		BasicDBObject queryResult = (BasicDBObject) JSON.parse(exchange.getIn().getBody(String.class));
		int docsUpdated = queryResult.getInt("n");
		if (docsUpdated > 0) {
			exchange.setProperty("delayPost", false);
		} else {
			exchange.setProperty("delayPost", true);
		}
	}
}