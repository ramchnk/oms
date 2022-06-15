package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mudra.sellinall.database.DbUtilities;

public class UpdatePromotionImportStatus implements Processor {

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		BasicDBObject searchQuery = new BasicDBObject();
		ObjectId objId = new ObjectId(accountNumber);
		searchQuery.put("_id", objId);
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("nickName.id", nickNameID);
		searchQuery.put("lazada", new BasicDBObject("$elemMatch", elemMatch));
		BasicDBObject updateQuery = new BasicDBObject();
		updateQuery.put("lazada.$.isPromotionImportProcessing",
				exchange.getProperty("isPromotionImportProcessing", Boolean.class));
		updateQuery.put("lazada.$.lastPromotionImportedTime", System.currentTimeMillis() / 1000);
		BasicDBObject setQuery = new BasicDBObject();
		setQuery.put("$set", updateQuery);
		DBCollection table = DbUtilities.getDBCollection("accounts");
		table.update(searchQuery, setQuery);
	}

}
