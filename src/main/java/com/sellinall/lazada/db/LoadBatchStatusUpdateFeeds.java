package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mudra.sellinall.database.DbUtilities;

public class LoadBatchStatusUpdateFeeds implements Processor {
	static Logger log = Logger.getLogger(LoadBatchStatusUpdateFeeds.class.getName());

	public void process(Exchange exchange) throws Exception {
		exchange.setProperty("feedHasSKUs", false);
		exchange.setProperty("isBulkStatusUpdate", true);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		searchQuery.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		searchQuery.put("requestType", "batchStatusUpdate");

		DBCollection table = DbUtilities.getInventoryDBCollection("lazadaFeeds");
		BasicDBObject feedList = (BasicDBObject) table.findOne(searchQuery);

		if (feedList != null) {
			List<String> customSKUs = new ArrayList<String>();
			JSONArray updateStatusList = new JSONArray(feedList.get("updateStatusList").toString());
			if (updateStatusList.length() != 0) {
				exchange.setProperty("updateStatusList", updateStatusList);
				exchange.setProperty("feedHasSKUs", true);
				exchange.setProperty("requestPayload", new ArrayList<String>());
				exchange.setProperty("feedMessages", new ArrayList<BasicDBObject>());
				updateFeedList(feedList, searchQuery);
			}

			Set<String> activeSellerSKUList = new HashSet<String>();
			Set<String> inactiveSellerSKUList = new HashSet<String>();
			Map<String, JSONObject> sellerSKUFeedMap = new LinkedHashMap<String, JSONObject>();

			for (int index = 0; index < updateStatusList.length(); index++) {
				JSONObject updateStatusObject = updateStatusList.getJSONObject(index);
				String customSKU = updateStatusObject.getString("customSKU");
				customSKUs.add(customSKU);
				if (sellerSKUFeedMap.containsKey(customSKU)) {
					JSONObject feedObj = sellerSKUFeedMap.get(customSKU);
					if (!feedObj.has("rowIdentifier") || updateStatusObject.has("rowIdentifier")) {
						/*
						 * Note: if auto status update & batch update processing at same time, then we
						 * will prioritise batch request & send response to batch sheet
						 */
						sellerSKUFeedMap.put(customSKU, updateStatusObject);
					}
				} else {
					sellerSKUFeedMap.put(customSKU, updateStatusObject);
				}
				if (updateStatusObject.getString("updateToStatus").equals("active")) {
					activeSellerSKUList.add(customSKU);
					inactiveSellerSKUList.remove(customSKU);
				} else if (updateStatusObject.getString("updateToStatus").equals("inactive")) {
					inactiveSellerSKUList.add(customSKU);
					activeSellerSKUList.remove(customSKU);
				}
			}

			exchange.setProperty("customSKUs", customSKUs);
			exchange.setProperty("inactiveSellerSKUList", inactiveSellerSKUList);
			exchange.setProperty("activeSellerSKUList", activeSellerSKUList);
			exchange.setProperty("sellerSKUFeedMap", sellerSKUFeedMap);
			exchange.getOut().setBody(updateStatusList);
		}
	}

	private void updateFeedList(BasicDBObject feedList, BasicDBObject searchQuery) {
		DBCollection table = DbUtilities.getInventoryDBCollection("lazadaFeeds");
		BasicDBObject SKUList = new BasicDBObject("updateStatusList",
				new BasicDBObject("$in", feedList.get("updateStatusList")));
		BasicDBObject updateData = new BasicDBObject("$pull", SKUList);
		table.update(searchQuery, updateData);
	}
}
