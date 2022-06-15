package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.lazada.util.LazadaUtil;

public class LoadOrderListWithOrderItemIDs implements Processor {
	static Logger log = Logger.getLogger(LoadOrderListWithOrderItemIDs.class.getName());

	public void process(Exchange exchange) throws Exception {
		BasicDBObject searchQuery = new BasicDBObject();
		String nickNameId = exchange.getProperty("nickNameID", String.class);
		exchange.setProperty("nickNameID", nickNameId);
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		searchQuery.put("site.nickNameID", nickNameId);
		JSONArray orderIds = exchange.getProperty("orderIDs", JSONArray.class);
		ArrayList<String> orderIDs = new ArrayList<String>();
		for (int i = 0; i < orderIds.length(); i++) {
			orderIDs.add(orderIds.getString(i));
		}
		searchQuery.put("orderItems.orderItemID", new BasicDBObject("$in", orderIDs));
		log.debug(searchQuery.toString());
		BasicDBObject projection = new BasicDBObject();
		projection.put("_id", 0);
		projection.put("notificationID", 0);
		DBObject sort = new BasicDBObject();
		sort.put("timeOrderCreated", -1);
		List<DBObject> ordersListFromDB = getOrdersListFromDB(searchQuery, projection, sort).toArray();
		JSONArray ordersArray = new JSONArray();
		for (DBObject order : ordersListFromDB) {
			ordersArray.put(LazadaUtil.parseToJsonObject(order));
		}
		exchange.getOut().setBody(ordersArray);
	}

	private static DBCursor getOrdersListFromDB(DBObject searchQuery, DBObject projection, DBObject sort) {
		DBCollection table = DbUtilities.getOrderDBCollection("order");
		if (sort != null) {
			return table.find(searchQuery, projection).sort(sort);
		}
		return table.find(searchQuery, projection);
	}

}
