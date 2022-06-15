package com.sellinall.lazada.db;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mudra.sellinall.database.DbUtilities;

public class GetChildSKUs implements Processor {
	static Logger log = Logger.getLogger(GetChildSKUs.class.getName());

	public void process(Exchange exchange) throws Exception {
		DBObject outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private DBObject createBody(Exchange exchange) {
		String SKU = (String) exchange.getProperty("SKU");
		String accountNumber = (String) exchange.getProperty("accountNumber");

		DBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("SKU", Pattern.compile("^" + SKU + "-.*"));
		searchQuery.put("lazada", new BasicDBObject("$exists", true));
		List<String> siteNicknameList = (List<String>) JSON.parse(exchange.getProperty("siteNicknames").toString());
		String nickNameId = siteNicknameList.get(0);
		searchQuery.put("lazada.nickNameID", nickNameId);
		DBObject projection = new BasicDBObject();
		projection.put("lazada.$", 1);
		projection.put("customSKU", 1);
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		List<DBObject> inventoryList = (List<DBObject>) table.find(searchQuery, projection).toArray();
		exchange.setProperty("inventoryList", inventoryList);
		return searchQuery;
	}

}
