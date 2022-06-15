package com.sellinall.lazada.db;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.util.enums.SIAInventoryStatus;

public class LoadChildInventory implements Processor {

	static Logger log = Logger.getLogger(LoadChildInventory.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject itemFromSite = exchange.getProperty("item", JSONObject.class);
		buildQuery(exchange, itemFromSite);
	}

	private void buildQuery(Exchange exchange, JSONObject item) throws JSONException {
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		Map<String, Integer> refrenceIDAndSKUFromDB = exchange.getProperty("refrenceIDAndSKUFromDB", Map.class);
		String parentSKU = exchange.getProperty("parentSKU", String.class);
		BasicDBObject searchQuery = new BasicDBObject();
		BasicDBObject fieldsFilter = new BasicDBObject();
		BasicDBObject elemMatch = new BasicDBObject();
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		searchQuery.put("SKU", Pattern.compile("^" + parentSKU.split("-")[0] + ".*"));
		searchQuery.put("status", new BasicDBObject("$ne", SIAInventoryStatus.REMOVED.toString()));
		elemMatch.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		elemMatch.put("variants", new BasicDBObject("$exists", false));
		BasicDBObject searchLazada = new BasicDBObject("$elemMatch", elemMatch);
		searchQuery.put("lazada", searchLazada);
		fieldsFilter.put("SKU", 1);
		fieldsFilter.put("lazada.refrenceID", 1);
		List<DBObject> inventoryList = table.find(searchQuery, fieldsFilter).toArray();
		for (int i = 0; i < inventoryList.size(); i++) {
			BasicDBObject inventory = (BasicDBObject) inventoryList.get(i);
			if (inventory.getString("SKU").contains("-")) {
				int SKU = Integer.parseInt(inventory.getString("SKU").split("-")[1]);
				List<BasicDBObject> lazada = (List<BasicDBObject>) inventory.get("lazada");
				refrenceIDAndSKUFromDB.put(lazada.get(0).getString("refrenceID"), SKU);
			}
		}
		exchange.setProperty("refrenceIDAndSKUFromDB", refrenceIDAndSKUFromDB);
	}
}