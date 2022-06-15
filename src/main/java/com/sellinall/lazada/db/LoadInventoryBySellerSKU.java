package com.sellinall.lazada.db;

import java.util.Arrays;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.util.enums.SIAInventoryStatus;

public class LoadInventoryBySellerSKU implements Processor {

	@SuppressWarnings("unchecked")
	@Override
	public void process(Exchange exchange) throws Exception {
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String channel = nickNameID.split("-")[0];
		List<String> sellerSKUs = (List<String>) exchange.getProperty("sellerSKUList");
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		searchQuery.put(channel + ".nickNameID", nickNameID);
		searchQuery.put("customSKU", new BasicDBObject("$in", sellerSKUs));
		BasicDBObject elemMatchQuery = new BasicDBObject("nickNameID", nickNameID);
		elemMatchQuery.put("status", new BasicDBObject("$in", Arrays.asList(SIAInventoryStatus.ACTIVE.toString(),
				SIAInventoryStatus.INACTIVE.toString(), SIAInventoryStatus.NOT_LISTED.toString())));
		elemMatchQuery.put("variants", new BasicDBObject("$exists", false));
		searchQuery.put(channel, new BasicDBObject("$elemMatch", elemMatchQuery));
		BasicDBObject projection = new BasicDBObject();
		projection.put("_id", 0);
		projection.put("SKU", 1);
		projection.put("customSKU", 1);
		projection.put(channel + ".$", 1);
		List<DBObject> inventoryList = table.find(searchQuery, projection).toArray();
		exchange.setProperty("inventoryList", inventoryList);
	}

}
