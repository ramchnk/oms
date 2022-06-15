package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.config.Config;
import com.mudra.sellinall.database.DbUtilities;

public class InsertNotificationIntoDB implements Processor {

	static Logger log = Logger.getLogger(InsertNotificationIntoDB.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		JSONObject notificationMsg = exchange.getIn().getBody(JSONObject.class);
		String notificationType = exchange.getProperty("notificationType", String.class);

		DBCollection table = DbUtilities.getDBNotificationCollection("lazadaNotifications");
		if (exchange.getProperties().containsKey("commonAccountDetails")) {
			List<DBObject> commonAccountDetails = exchange.getProperty("commonAccountDetails", List.class);
			List<DBObject> insertObjList = new ArrayList<DBObject>();
			for (int i = 0; i < commonAccountDetails.size(); i++) {
				DBObject insertObj = constructNotificationDocument(notificationMsg, commonAccountDetails.get(i),
						notificationType);
				log.info("inserting notifications for shared account - accountNumber : "
						+ insertObj.get("accountNumber") + ", nickNameID : " + insertObj.get("nickNameID")
						+ ", orderID : " + insertObj.get("orderID"));
				insertObjList.add(insertObj);
			}
			table.insert(insertObjList);
		} else {
			DBObject user = exchange.getProperty("UserDetails", DBObject.class);
			DBObject insertObj = constructNotificationDocument(notificationMsg, user, notificationType);
			table.insert(insertObj);
		}
	}

	private DBObject constructNotificationDocument(JSONObject notificationMsg, DBObject user, String notificationType)
			throws JSONException {
		JSONObject data = notificationMsg.getJSONObject("data");

		DBObject lazadaObj = (DBObject) user.get("lazada");
		DBObject nickNameObj = (DBObject) lazadaObj.get("nickName");

		DBObject doc = new BasicDBObject();
		doc.put("accountNumber", user.get("_id").toString());
		doc.put("nickNameID", nickNameObj.get("id").toString());
		doc.put("notificationType", notificationType);
		doc.put("rawData", BasicDBObject.parse(notificationMsg.toString()));
		if (notificationType.equals("order")) {
			doc.put("orderID", data.getString("trade_order_id"));
		}
		if (notificationMsg.has("countryCode")) {
			doc.put("countryCode", notificationMsg.getString("countryCode"));
		} else {
			doc.put("countryCode", lazadaObj.get("countryCode").toString());
		}
		return doc;
	}

}
