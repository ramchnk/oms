/**
 * 
 */
package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.sellinall.util.enums.SIAInventoryStatus;

/**
 * @author Senthil
 * 
 */
public class CreateSKUPostingStatusQuery implements Processor {
	static Logger log = Logger.getLogger(CreateSKUPostingStatusQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		Object[] outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private Object[] createBody(Exchange exchange) {
		String SKU = (String) exchange.getProperty("SKU");
		String accountNumber = (String) exchange.getProperty("accountNumber");
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		log.debug("createSKUPostingStatusQuery sku = " + SKU);

		BasicDBList and = new BasicDBList();
		and.add(new BasicDBObject("accountNumber", accountNumber));
		and.add(new BasicDBObject("SKU", SKU));
		String channelName = exchange.getProperty("channelName", String.class);
		and.add(new BasicDBObject(channelName + ".nickNameID", nickNameID));

		DBObject searchQuery = new BasicDBObject();
		searchQuery.put("$and", and);

		BasicDBObject update = new BasicDBObject(channelName + ".$.updateStatus", SIAInventoryStatus.POSTING.toString());

		DBObject updateObject = new BasicDBObject("$set", update);
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
		return new Object[] { searchQuery, updateObject };

	}
}