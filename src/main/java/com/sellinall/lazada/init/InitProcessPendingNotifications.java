package com.sellinall.lazada.init;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.mongodb.DBObject;

public class InitProcessPendingNotifications implements Processor {

	static Logger log = Logger.getLogger(InitProcessPendingNotifications.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		DBObject userDetails = exchange.getProperty("UserDetails", DBObject.class);
		DBObject inBody = exchange.getIn().getBody(DBObject.class);
		DBObject indexObj = (DBObject) inBody.get("_id");
		List<String> statuses = (List<String>) inBody.get("statuses");
		List<ObjectId> docIDs = (List<ObjectId>) inBody.get("docIDs");

		exchange.setProperty("orderID", indexObj.get("orderID"));
		exchange.setProperty("countryCode", indexObj.get("countryCode"));
		exchange.setProperty("statuses", statuses);
		exchange.setProperty("docIDs", docIDs);
		exchange.setProperty("needToUpdateDocumentUrl", statuses.contains("repacked") ? true : false);
		exchange.setProperty("notificationType", "order");
		exchange.getOut().setBody(userDetails);

		log.info("processing order notification for accountNumber : "
				+ exchange.getProperty("accountNumber", String.class) + ", nickNameID : "
				+ exchange.getProperty("nickNameID", String.class) + " & request : " + inBody);
	}

}
