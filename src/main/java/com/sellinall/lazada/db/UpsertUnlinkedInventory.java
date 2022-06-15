/**
 * 
 */
package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.WriteConcern;

/**
 * @author Senthil
 * 
 */
public class UpsertUnlinkedInventory implements Processor {
	static Logger log = Logger.getLogger(UpsertUnlinkedInventory.class.getName());

	public void process(Exchange exchange) throws Exception {
		BasicDBObject unlinkedInventory = exchange.getIn().getBody(BasicDBObject.class);
		String channelName = exchange.getProperty("channelName", String.class);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		searchQuery.put(channelName + ".nickNameID", exchange.getProperty("nickNameID", String.class));
		unlinkedInventory.remove("refrenceID");
		searchQuery.put("SKU", unlinkedInventory.getString("SKU"));
		BasicDBObject updateData = new BasicDBObject("$set", unlinkedInventory);
		log.debug("SearchQuery : " + searchQuery + " " + updateData);
		exchange.getOut().setHeader(MongoDbConstants.UPSERT, true);
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
		exchange.getOut().setBody(new Object[] { searchQuery, updateData });
	}

}