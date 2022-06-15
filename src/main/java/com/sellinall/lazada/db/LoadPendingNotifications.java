package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;

public class LoadPendingNotifications implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);

		DBObject match = new BasicDBObject();
		match.put("accountNumber", accountNumber);
		match.put("nickNameID", nickNameID);

		DBObject indexObj = new BasicDBObject();
		indexObj.put("orderID", "$orderID");
		indexObj.put("countryCode", "$countryCode");

		DBObject group = new BasicDBObject();
		group.put("_id", indexObj);
		group.put("docIDs", new BasicDBObject("$addToSet", "$_id"));
		group.put("statuses", new BasicDBObject("$addToSet", "$rawData.data.order_status"));

		List<DBObject> aggregateQuery = new ArrayList<DBObject>();
		aggregateQuery.add(new BasicDBObject("$match", match));
		aggregateQuery.add(new BasicDBObject("$group", group));

		DBCollection table = DbUtilities.getDBNotificationCollection("lazadaNotifications");
		List<DBObject> notifications = (List<DBObject>) table.aggregate(aggregateQuery).results();
		exchange.getOut().setBody(notifications);
	}

}
