package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.util.DateUtil;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.SIAPaymentStatus;

public class LoadUnPaidOrdersFromDB implements Processor {
	static Logger log = Logger.getLogger(LoadUnPaidOrdersFromDB.class.getName());
	public static final long THIRTY_MINS = 1800;

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		long timeOrderCreatedBefore = (Long) DateUtil.getSIADateFormat() - THIRTY_MINS;
		ArrayList<DBObject> orderList = loadOrderFromDb(accountNumber, nickNameID, timeOrderCreatedBefore);
		exchange.getOut().setBody(orderList);
	}

	private ArrayList<DBObject> loadOrderFromDb(String accountNumber, String nickNameID, long timeOrderCreatedBefore) {
		DBCollection table = DbUtilities.getOrderDBCollection("order");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("site.nickNameID", nickNameID);
		searchQuery.put("orderStatuses", SIAOrderStatus.INITIATED.toString());
		searchQuery.put("paymentStatuses", SIAPaymentStatus.NOT_INITIATED.toString());
		List<String> paymentMethods = new ArrayList<String>();
		paymentMethods.add("COD");
		searchQuery.put("paymentMethods", new BasicDBObject("$ne", paymentMethods));
		searchQuery.put("timeOrderCreated", new BasicDBObject("$lte", timeOrderCreatedBefore));
		BasicDBObject fields = new BasicDBObject();
		fields.put("_id", 0);
		fields.put("updateStatus", 0);
		return (ArrayList<DBObject>) table.find(searchQuery, fields).toArray();
	}
}
