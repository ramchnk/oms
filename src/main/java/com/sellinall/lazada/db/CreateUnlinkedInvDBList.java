/**
 * 
 */
package com.sellinall.lazada.db;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;

/**
 * @author Senthil
 * 
 */
public class CreateUnlinkedInvDBList implements Processor {
	static Logger log = Logger.getLogger(CreateUnlinkedInvDBList.class.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		List<BasicDBObject> unlinkedInventoryDocs = (List<BasicDBObject>) exchange.getProperty("unlinkedInventoryDocs");
		exchange.getOut().setBody(unlinkedInventoryDocs);
	}
}