/**
 * 
 */
package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.mudra.sellinall.database.DbUtilities;

/**
 * @author vikraman
 *
 */
public class UpdateUserDBLastScannedTime implements Processor {
	static Logger log = Logger.getLogger(UpdateUserDBLastScannedTime.class.getName());

	public void process(Exchange exchange) throws Exception {
		String outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private String createBody(Exchange exchange) {
		DBCollection table = DbUtilities.getDBCollection("accounts");
		ObjectId objid = new ObjectId((String) exchange.getProperty("accountNumber"));
		DBObject userDBObject = (DBObject) exchange.getProperty("UserDetails");
		String channelName=exchange.getProperty("channelName",String.class);
		BasicDBObject channel = (BasicDBObject) (userDBObject.get(channelName));
		BasicDBObject nickName = (BasicDBObject) channel.get("nickName");
		String nickNameId = nickName.getString("id");

		DBObject filterField1 = new BasicDBObject("_id", objid);
		DBObject filterField2 = new BasicDBObject(channelName+".nickName.id", nickNameId);
		BasicDBList and = new BasicDBList();
		and.add(filterField1);
		and.add(filterField2);
		DBObject filterField = new BasicDBObject("$and", and);

		BasicDBObject updateFields = new BasicDBObject();
		if (exchange.getProperty("requestType", String.class).equals("scanNewOrders")) {
			updateFields.put(channelName + ".$.lastNewOrderScannedTime",
					exchange.getProperty("lastRequestEndTime", Long.class));
		} else if (exchange.getProperty("requestType", String.class).equals("scanUpdatedOrders")) {
			updateFields.put(channelName + ".$.lastUpdatedOrderScannedTime",
					exchange.getProperty("lastRequestEndTime", Long.class));
		} else if (exchange.getProperty("requestType", String.class).equals("processPendingNotification")) {
			updateFields.put(channelName + ".$.lastOrderNotificationProcessedTime", System.currentTimeMillis() / 1000L);
		} else {
			updateFields.put(channelName + ".$.lastScannedTime",
					exchange.getProperty("lastRequestEndTime", Long.class));
		}

		DBObject updateQuery = new BasicDBObject();
		updateQuery.put("$set", updateFields);
		log.debug("Updated user db with last Scanned time");
		WriteResult output = table.update(filterField, updateQuery);
		return output.toString();
	}
}