package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.enums.SIAInventoryStatus;

public class LoadInventoryWithSellerSKU implements Processor {
	static Logger log = Logger.getLogger(LoadInventoryWithSellerSKU.class.getName());

	public void process(Exchange exchange) throws Exception {
		exchange.setProperty("isEligibleToProceed", false);
		BasicDBObject projection = new BasicDBObject("lazada.status", 1);
		projection.put("SKU", 1);
		projection.put("customSKU", 1);
		List<DBObject> aggregateQuery = createQuery(exchange);
		DBCollection table = DbUtilities.getROInventoryDBCollection("inventory");
		List<DBObject> inventoryList = (List<DBObject>) table.aggregate(aggregateQuery).results();
		List<String> customSKUsList = exchange.getProperty("customSKUs", ArrayList.class);

		if (inventoryList.size() == 0) {
			exchange.setProperty("isInventoryListEmpty", true);
			return;
		}
		if (inventoryList.size() != customSKUsList.size()) {
			List<String> notFoundSKUsList = new ArrayList<String>();
			notFoundSKUsList.addAll(customSKUsList);
			List<String> availableCustomSKUs = new ArrayList<String>();
			for (DBObject inv : inventoryList) {
				JSONObject invObject = LazadaUtil.parseToJsonObject(inv);
				JSONArray documentsList = (JSONArray) invObject.get("documents");
				for (int i = 0; i < documentsList.length(); i++) {
					JSONObject documentsObject = documentsList.getJSONObject(i);
					if (customSKUsList.contains(documentsObject.get("customSKU"))) {
						availableCustomSKUs.add(documentsObject.getString("customSKU"));
					}
				}
			}
			notFoundSKUsList.removeAll(availableCustomSKUs);
			exchange.setProperty("notFoundSKUsList", notFoundSKUsList);
		}
		exchange.setProperty("isEligibleToProceed", true);
		exchange.getOut().setBody(inventoryList);
	}

	private List<DBObject> createQuery(Exchange exchange) throws JSONException {
		List<String> statusList = new ArrayList<String>(Arrays.asList(SIAInventoryStatus.ACTIVE.toString(),
				SIAInventoryStatus.INACTIVE.toString(), SIAInventoryStatus.SOLDOUT.toString()));
		String accountNumber = (String) exchange.getProperty("accountNumber");
		List<String> customSKUs = exchange.getProperty("customSKUs", ArrayList.class);

		DBObject matchQuery = new BasicDBObject();
		matchQuery.put("accountNumber", accountNumber);
		matchQuery.put("customSKU", new BasicDBObject("$in", customSKUs));
		DBObject elemMatch = new BasicDBObject();
		elemMatch.put("status", new BasicDBObject("$in", statusList));
		elemMatch.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		// only load non variant and variant child record
		elemMatch.put("variants", new BasicDBObject("$exists", false));
		matchQuery.put("lazada", new BasicDBObject("$elemMatch", elemMatch));

		List<Object> setObj = new ArrayList<Object>();
		setObj.add("$SKU");
		setObj.add(0);
		setObj.add(10);
		DBObject indexObj = new BasicDBObject();
		indexObj.put("$substr", setObj);

		DBObject groupQuery = new BasicDBObject();
		groupQuery.put("_id", indexObj);
		groupQuery.put("documents", new BasicDBObject("$addToSet", "$$ROOT"));

		List<DBObject> aggregateQuery = new ArrayList<DBObject>();
		aggregateQuery.add(new BasicDBObject("$match", matchQuery));
		aggregateQuery.add(new BasicDBObject("$group", groupQuery));

		log.debug("search query" + aggregateQuery.toString());
		return aggregateQuery;

	}

}
