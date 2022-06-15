package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import com.mudra.sellinall.database.DbUtilities;

public class loadStockUpdateFeeds implements Processor {
	static Logger log = Logger.getLogger(loadStockUpdateFeeds.class.getName());

	public void process(Exchange exchange) throws Exception {

		exchange.setProperty("feedHasSKUs", false);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		searchQuery.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		searchQuery.put("requestType", "stockUpdate");

		DBCollection table = DbUtilities.getROInventoryDBCollection("lazadaFeeds");
		BasicDBObject feedList = (BasicDBObject) table.findOne(searchQuery);

		if (feedList != null) {
			List<String> customSKUs = new ArrayList<String>();
			JSONArray updateStockList = new JSONArray(feedList.get("updateStockList").toString());
			if (updateStockList.length() != 0) {
				exchange.setProperty("updateStockList", updateStockList);
				exchange.setProperty("feedHasSKUs", true);
				updateFeedList(feedList, searchQuery);
			}

			Map<String, JSONObject> skuFeedMap = new LinkedHashMap<String, JSONObject>();
			for (int index = 0; index < updateStockList.length(); index++) {
				JSONObject updateStatusObject = updateStockList.getJSONObject(index);
				customSKUs.add(updateStatusObject.getString("sellerSKU"));
				skuFeedMap.put(updateStatusObject.getString("SKU"), updateStatusObject);
			}

			exchange.setProperty("customSKUs", customSKUs);
			exchange.setProperty("skuFeedMap", skuFeedMap);
			exchange.getOut().setBody(updateStockList);
		}
	}

	private void updateFeedList(BasicDBObject feedList, BasicDBObject searchQuery) {
		BasicDBObject SKUList = new BasicDBObject("updateStockList",
				new BasicDBObject("$in", feedList.get("updateStockList")));
		BasicDBObject updateData = new BasicDBObject("$pull", SKUList);
		DBCollection table = DbUtilities.getInventoryDBCollection("lazadaFeeds");
		WriteResult output = table.update(searchQuery, updateData);
		if (output.getN() == 0) {
			log.error("Failed while pull feed data for accountNumber : " + searchQuery.getString("accountNumber")
					+ " nickNameID : " + searchQuery.getString("nickNameID") + ", searchQuery : " + searchQuery
					+ " & upsertData : " + updateData);
		}
	}
}
