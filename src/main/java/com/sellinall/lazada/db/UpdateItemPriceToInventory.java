package com.sellinall.lazada.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class UpdateItemPriceToInventory implements Processor {
	static Logger log = Logger.getLogger(UpdateItemPriceToInventory.class.getName());

	@Override
	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {

		String failureReason = "";
		if (exchange.getProperties().containsKey("failureReason")) {
			failureReason = exchange.getProperty("failureReason", String.class);
		}
		HashMap<String, JSONArray> sellerUpdateMessageMap = exchange.getProperty("sellerUpdateMessageMap",
				HashMap.class);
		Map<String, JSONObject> sellerSKUPriceDetailsMap = exchange.getProperty("sellerSKUPriceDetailsMap",
				HashMap.class);

		List<DBObject> inventoryList = (List<DBObject>) exchange.getProperty("inventoryList");
		for (DBObject inventoryObj : inventoryList) {
			BasicDBObject inventory = (BasicDBObject) inventoryObj;
			String customSKU = inventory.getString("customSKU");
			String SKU = inventory.getString("SKU");
			JSONArray oldMessageArray = new JSONArray();
			JSONObject oldMessageObject = new JSONObject();
			if (sellerUpdateMessageMap.containsKey(customSKU)) {
				oldMessageArray = sellerUpdateMessageMap.get(customSKU);
			}
			oldMessageObject.put("SKU", SKU);
			Map<String, String> sellerUpdateFailureMessageMap = exchange.getProperty("sellerUpdateFailureMessageMap",
					HashMap.class);
			if (!failureReason.isEmpty() || sellerUpdateFailureMessageMap.containsKey(customSKU)) {
				String errorMessage = !failureReason.isEmpty() ? failureReason
						: sellerUpdateFailureMessageMap.get(customSKU);
				oldMessageObject.put("isPostedSuccefully", false);
				oldMessageObject.put("updateMessage", errorMessage);
			} else {
				oldMessageObject.put("isPostedSuccefully", true);
				oldMessageObject.put("updateMessage", "Updated Successfully");
				updatePriceToInventory(exchange, sellerSKUPriceDetailsMap, SKU, customSKU, failureReason);
			}
			oldMessageArray.put(oldMessageObject);
			sellerUpdateMessageMap.put(customSKU, oldMessageArray);
		}
	}

	public void updatePriceToInventory(Exchange exchange, Map<String, JSONObject> sellerSKUPriceDetailsMap, String SKU,
			String sellerSKU, String failureReason) throws JSONException {
		JSONObject sellerSKUObject = sellerSKUPriceDetailsMap.get(sellerSKU);
		JSONObject itemAmount = sellerSKUObject.getJSONObject("itemAmount");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber"));
		searchQuery.put("SKU", SKU);
		searchQuery.put("lazada.nickNameID", exchange.getProperty("nickNameID"));
		BasicDBObject updateObject = new BasicDBObject();
		updateObject.put("lazada.$.itemAmount.amount", itemAmount.getLong("amount"));
		if (sellerSKUObject.has("salePrice")) {
			JSONObject salePrice = sellerSKUObject.getJSONObject("salePrice");
			updateObject.put("lazada.$.salePrice.amount", salePrice.getLong("amount"));
			updateObject.put("lazada.$.salePrice.currencyCode", salePrice.getString("currencyCode"));
			updateObject.put("lazada.$.saleStartDate", sellerSKUObject.getLong("saleStartDate"));
			updateObject.put("lazada.$.saleEndDate", sellerSKUObject.getLong("saleEndDate"));
		}
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		log.debug("searchQuery :" + searchQuery.toString() + " updateObject:" + updateObject.toString());
		table.update(searchQuery, new BasicDBObject("$set", updateObject));
	}
}
