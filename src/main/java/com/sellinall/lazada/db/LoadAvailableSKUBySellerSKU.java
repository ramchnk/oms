package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.util.enums.SIAInventoryStatus;

public class LoadAvailableSKUBySellerSKU implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject input = exchange.getProperty("inputRequest", JSONObject.class);
		exchange.setProperty("sellerSKU", input.getString("sellerSKU"));
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		List<String> skuList = new ArrayList<String>();
		List<String> status = new ArrayList<String>();
		status.add(SIAInventoryStatus.REMOVED.toString());
		status.add(SIAInventoryStatus.FAILED.toString());

		DBObject matchQuery = new BasicDBObject();
		matchQuery.put("accountNumber", accountNumber);
		matchQuery.put("customSKU", input.getString("sellerSKU"));
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("nickNameID", nickNameID);
		elemMatch.put("status", new BasicDBObject("$nin", status));
		matchQuery.put("lazada", new BasicDBObject("$elemMatch", elemMatch));

		DBObject projection = new BasicDBObject();
		projection.put("_id", 0);
		projection.put("SKU", 1);

		DBObject groupQuery = new BasicDBObject();
		groupQuery.put("_id", null);
		groupQuery.put("skuList", new BasicDBObject("$addToSet", "$SKU"));

		DBCollection invCollection = DbUtilities.getROInventoryDBCollection("inventory");
		AggregationOutput result = invCollection.aggregate(new BasicDBObject("$match", matchQuery),
				new BasicDBObject("$project", projection), new BasicDBObject("$group", groupQuery));
		if (result != null && ((ArrayList<DBObject>) result.results()).size() > 0) {
			DBObject resultObj = ((ArrayList<DBObject>) result.results()).get(0);
			if (resultObj.containsField("skuList")) {
				skuList = (List<String>) resultObj.get("skuList");
			}
		}
		exchange.setProperty("skuListToProcess", skuList);
	}

}
