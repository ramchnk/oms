package com.sellinall.lazada.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;

public class ProcessPromotionDetails {

	public static Map<String, JSONObject> getSKUIDDtails(List<String> allSKUIDs, String accountNumber, String nickNameID)
			throws JSONException {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("nickNameID", nickNameID);
		elemMatch.put("skuID", new BasicDBObject("$in", allSKUIDs));
		searchQuery.put("lazada", new BasicDBObject("$elemMatch", elemMatch));
		BasicDBObject projection = new BasicDBObject();
		projection.put("SKU", 1);
		projection.put("customSKU", 1);
		projection.put("imageURL", 1);
		projection.put("lazada.itemUrl", 1);
		projection.put("lazada.imageURI", 1);
		projection.put("lazada.itemTitle", 1);
		projection.put("lazada.variantDetails", 1);
		projection.put("lazada.skuID", 1);
		projection.put("lazada.itemAmount", 1);
		projection.put("lazada.$", 1);
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		List<DBObject> inventoryDetails = table.find(searchQuery, projection).toArray();
		return mappingItemDetailsWithSKUID(allSKUIDs, inventoryDetails);
	}
	public static Map<String, JSONObject> mappingItemDetailsWithSKUID(List<String> skuIDs,
			List<DBObject> inventoryDetails) throws JSONException {
		Map<String, JSONObject> itemDetailsMap = new HashMap<String, JSONObject>();
		for (DBObject inventory : inventoryDetails) {
			if (inventory.containsField("lazada")) {
				List<DBObject> channelItemDetailForFilter = (List<DBObject>) inventory.get("lazada");
				DBObject channelItemDetail = channelItemDetailForFilter.get(0);
				if (channelItemDetail.containsField("skuID")) {
					if (skuIDs.contains(channelItemDetail.get("skuID"))) {
						JSONObject updateInventory = new JSONObject();
						updateInventory.put("SKU", inventory.get("SKU"));
						updateInventory.put("sellerSKU", inventory.get("customSKU"));
						updateInventory.put("SKUID", channelItemDetail.get("skuID"));

						if (channelItemDetail.containsField("itemUrl")) {
							updateInventory.put("itemUrl", channelItemDetail.get("itemUrl"));
						}
						if (inventory.containsField("imageURL")) {
							String imageURL = (String) inventory.get("imageURL");
							if (channelItemDetail.containsField("imageURI")) {
								List<String> imageURIArray = (List<String>) channelItemDetail.get("imageURI");
								if (imageURIArray.size() > 0) {
									updateInventory.put("imageURL", imageURL + imageURIArray.get(0));
								}
							}
						}
						if (channelItemDetail.containsField("itemTitle")) {
							updateInventory.put("itemTitle", channelItemDetail.get("itemTitle"));
						}
						if (channelItemDetail.containsField("variantDetails")) {
							updateInventory.put("variantDetails", LazadaUtil
									.parseListToJsonArray((BasicDBList) channelItemDetail.get("variantDetails")));
						}
						if (channelItemDetail.containsField("itemAmount")) {
							updateInventory.put("itemOrginalAmount",
									LazadaUtil.parseToJsonObject((BasicDBObject) channelItemDetail.get("itemAmount")));
						}
						itemDetailsMap.put((String) channelItemDetail.get("skuID"), updateInventory);
					}
				}
			}
		}
		return itemDetailsMap;
	}

}
