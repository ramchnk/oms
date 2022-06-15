package com.sellinall.lazada.db;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.Config;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.util.enums.SIAInventoryStatus;

public class InsertStatusUpdateDocInDB implements Processor {
	static Logger log = Logger.getLogger(InsertStatusUpdateDocInDB.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = "";
		JSONObject sellerSKUObject = new JSONObject();
		String bulkUpdateType = exchange.getProperty("bulkUpdateType", String.class);

		if (bulkUpdateType.equals("status")) {
			JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
			String requestType = exchange.getProperty("requestType", String.class);
			sellerSKUObject.put("accountNumber", accountNumber);
			sellerSKUObject.put("needToUpdateStock", exchange.getProperty("needToUpdateStock"));
			if (requestType.equals("batchEditItem")) {
				nickNameID = inBody.getString("nickNameID");
				sellerSKUObject.put("customSKU", inBody.getString("sellerSKU"));
				sellerSKUObject.put("nickNameID", nickNameID);
				sellerSKUObject.put("userId", inBody.getString("userId"));
				sellerSKUObject.put("userName", inBody.getString("userName"));
				sellerSKUObject.put("rowIdentifier",
						(BasicDBObject) JSON.parse(inBody.getJSONObject("rowIdentifier").toString()));
				sellerSKUObject.put("updateToStatus", inBody.getString("updateToStatus"));
				sellerSKUObject.put("requestType", inBody.getString("requestType"));
			} else if (requestType.equals("updateItem")) {
				JSONObject lazadaObj = inBody.getJSONArray("lazada").getJSONObject(0);
				nickNameID = lazadaObj.getString("nickNameID");
				if (exchange.getProperties().containsKey("statusToUpdate")) {
					if (exchange.getProperty("statusToUpdate", String.class)
							.equals(SIAInventoryStatus.ACTIVE.toString())) {
						sellerSKUObject.put("updateToStatus", "active");
					} else {
						sellerSKUObject.put("updateToStatus", "inactive");
					}
				} else {
					if (lazadaObj.getString("status").equals(SIAInventoryStatus.ACTIVE.toString())) {
						sellerSKUObject.put("updateToStatus", "active");
					} else {
						sellerSKUObject.put("updateToStatus", "inactive");
					}
				}
				sellerSKUObject.put("customSKU", inBody.getString("customSKU"));
				sellerSKUObject.put("nickNameID", nickNameID);
				sellerSKUObject.put("requestType", requestType);
			} else {
				nickNameID = exchange.getProperty("nickNameID", String.class);
				sellerSKUObject.put("customSKU", exchange.getProperty("sellerSKU", String.class));
				sellerSKUObject.put("nickNameID", nickNameID);
				sellerSKUObject.put("updateToStatus", exchange.getProperty("statusToUpdate", String.class));
				sellerSKUObject.put("requestType", requestType);
			}
			updateStatusFeeds(accountNumber, nickNameID, sellerSKUObject);
		} else if(bulkUpdateType.equals("stock")) {
			nickNameID = exchange.getProperty("nickNameID", String.class);
			Map<String, JSONObject> skuFeedMap = exchange.getProperty("skuFeedMap", Map.class);
			BasicDBObject inventory = exchange.getProperty("skuDetails", BasicDBObject.class);
			String invSKU = inventory.getString("SKU");
			if (skuFeedMap.containsKey(invSKU)) {
				sellerSKUObject = skuFeedMap.get(invSKU);
				sellerSKUObject.put("customSKU", sellerSKUObject.getString("sellerSKU"));
			}
			sellerSKUObject.put("needToUpdateStock", false);
			if (sellerSKUObject.has("needToUpdateStatus") && sellerSKUObject.getBoolean("needToUpdateStatus")
					&& sellerSKUObject.has("updateToStatus")) {
				updateStatusFeeds(accountNumber, nickNameID, sellerSKUObject);
			}
		}
	}

	private void updateStatusFeeds(String accountNumber, String nickNameID, JSONObject sellerSKUObject) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("nickNameID", nickNameID);
		searchQuery.put("requestType", "batchStatusUpdate");

		DBObject updateData = new BasicDBObject();
		updateData.put("$push", new BasicDBObject("updateStatusList", BasicDBObject.parse(sellerSKUObject.toString())));
		if (Config.getConfig().getTestAccountNumber().equals(accountNumber)) {
			log.info("update status update feed data - searchQuery : " + searchQuery + " & updateData : " + updateData);
		}

		DBCollection table = DbUtilities.getInventoryDBCollection("lazadaFeeds");
		WriteResult output = table.update(searchQuery, updateData, true, false);
		if (output.getN() == 0) {
			log.error("Failed while upsert the feed data for accountNumber : " + accountNumber + " nickNameID : "
					+ nickNameID + ", searchQuery : " + searchQuery + " & upsertData : " + updateData);
		}
	}
}