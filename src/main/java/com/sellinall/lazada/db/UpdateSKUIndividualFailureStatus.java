/**
 * 
 */
package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import org.apache.log4j.Logger;

/**
 * @author vikraman
 * 
 */
public class UpdateSKUIndividualFailureStatus implements Processor {
	static Logger log = Logger.getLogger(UpdateSKUIndividualFailureStatus.class.getName());
	public void process(Exchange exchange) throws Exception {
		Object[] outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private Object[] createBody(Exchange exchange) {
		String SKU = exchange.getProperty("sellerSKU", String.class);
		String channelName = exchange.getProperty("channelName", String.class);
		DBObject filterField1 = new BasicDBObject("SKU", SKU);
		DBObject filterField2 = new BasicDBObject(channelName + ".nickNameID", exchange.getProperty("nickNameID",
				String.class));
		BasicDBList and = new BasicDBList();
		and.add(filterField1);
		and.add(filterField2);
		DBObject filterField = new BasicDBObject("$and", and);

		BasicDBObject update = new BasicDBObject();

		update.append(channelName + ".$.status", "F").append(channelName + ".$.failureReason",
				exchange.getProperty("failureReason"));

		DBObject updateObject = new BasicDBObject("$set", update);
		log.debug(filterField.toString());
		log.debug(updateObject.toString());
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
		return new Object[] { filterField, updateObject };
	}
}