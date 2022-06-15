package com.sellinall.lazada.db;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;

public class RemoveProcessedNotificationsFromDB implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		List<ObjectId> docIDs = exchange.getProperty("docIDs", List.class);

		DBObject query = new BasicDBObject();
		query.put("_id", new BasicDBObject("$in", docIDs));

		DBCollection table = DbUtilities.getDBNotificationCollection("lazadaNotifications");
		table.remove(query);
	}

}
