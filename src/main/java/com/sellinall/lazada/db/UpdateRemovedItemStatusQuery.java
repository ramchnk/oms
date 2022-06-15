package com.sellinall.lazada.db;

import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.sellinall.util.DateUtil;
import com.sellinall.util.enums.SIAInventoryStatus;

public class UpdateRemovedItemStatusQuery implements Processor {
	static Logger log = Logger.getLogger(UpdateSKUDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		Object[] outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private Object[] createBody(Exchange exchange) throws JSONException {
		BasicDBObject inventory = exchange.getProperty("inventory", BasicDBObject.class);
		DBObject query = new BasicDBObject();
		query.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		String SKU = exchange.getProperty("SKU").toString();
		if (inventory.containsField("variants")) {
			query.put("SKU", Pattern.compile("^" + SKU + ".*"));
		} else {
			query.put("SKU", SKU);
		}
		String channleName = exchange.getProperty("channelName", String.class);
		query.put(channleName + ".nickNameID", exchange.getProperty("nickNameID").toString());

		BasicDBObject setObject = new BasicDBObject();
		DBObject updateObject = new BasicDBObject();
		if (exchange.getProperties().containsKey("isItemDeletedFromMarketPlace")
				&& exchange.getProperty("isItemDeletedFromMarketPlace", Boolean.class)) {
			setObject.put(channleName + ".$.status", SIAInventoryStatus.REMOVED.toString());
			setObject.put("timeDeleted", DateUtil.getSIADateFormat());
		} else {
			setObject.put("status", SIAInventoryStatus.INITIATED.toString());
			updateObject.put("$unset", new BasicDBObject("timeDeleted", 1));
		}
		updateObject.put("$set", setObject);

		log.debug("Removed item update query = " + query.toString());
		log.debug("Removed item update data = " + updateObject.toString());
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
		exchange.getOut().setHeader(MongoDbConstants.MULTIUPDATE, true);
		return new Object[] { query, updateObject };
	}

}
