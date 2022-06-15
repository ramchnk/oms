package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;

public class UpdateIndividualSKUDBQuery implements Processor {
	static Logger log = Logger.getLogger(UpdateIndividualSKUDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		Object[] outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private Object[] createBody(Exchange exchange) {
		BasicDBObject setObject = (BasicDBObject) exchange.getIn().getBody();
		String SKU = setObject.getString("SKU");
		setObject.remove("SKU");

		String nickNameID = exchange.getProperty("nickNameID", String.class);
		DBObject searchQuery = new BasicDBObject("accountNumber", exchange.getProperty("accountNumber", String.class));
		searchQuery.put("SKU", SKU);
		searchQuery.put("lazada.nickNameID", nickNameID);

		DBObject updateObject = new BasicDBObject("$set", setObject);
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
		return new Object[] { searchQuery, updateObject };
	}
}
