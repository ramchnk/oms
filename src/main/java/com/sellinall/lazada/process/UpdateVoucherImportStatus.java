package com.sellinall.lazada.process;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mudra.sellinall.database.DbUtilities;

public class UpdateVoucherImportStatus implements Processor {

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		BasicDBObject searchQuery = new BasicDBObject();
		ObjectId objId = new ObjectId(accountNumber);
		searchQuery.put("_id", objId);
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("nickName.id", nickNameID);
		searchQuery.put("lazada", new BasicDBObject("$elemMatch", elemMatch));
		BasicDBObject updateObject = new BasicDBObject();
		updateObject.put("lazada.$.isVoucherImportProcessing",
				exchange.getProperty("isVoucherImportProcessing", Boolean.class));
		updateObject.put("lazada.$.lastVoucherImportedTime", System.currentTimeMillis() / 1000);
		DBCollection table = DbUtilities.getDBCollection("accounts");
		table.update(searchQuery, new BasicDBObject("$set", updateObject));
	}

}
