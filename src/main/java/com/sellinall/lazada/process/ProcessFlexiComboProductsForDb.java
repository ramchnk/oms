package com.sellinall.lazada.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.enums.PromotionType;

public class ProcessFlexiComboProductsForDb implements Processor {

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		JSONObject fexiComboItemsWithProduct = exchange.getProperty("fexiComboItemWithProduct", JSONObject.class);
		Map<String, DBObject> itemDetailsSKUIDmap = margeAllSKUIDsInSingleArray(fexiComboItemsWithProduct,
				accountNumber);
		convetToDBStructure(fexiComboItemsWithProduct, itemDetailsSKUIDmap, exchange);
	}

	public static Map<String, DBObject> margeAllSKUIDsInSingleArray(JSONObject flexiComboItem, String accountNumber)
			throws Exception {
		ArrayList<String> allSKUIDs = new ArrayList<String>();
		JSONArray emptyArray = new JSONArray();
		margeAllSKUIDsInSingleArrayForProduct(allSKUIDs,
				flexiComboItem.has("products") ? flexiComboItem.getJSONArray("products") : emptyArray,
				flexiComboItem.has("sample_skus") ? flexiComboItem.getJSONArray("sample_skus") : emptyArray,
				flexiComboItem.has("gift_skus") ? flexiComboItem.getJSONArray("gift_skus") : emptyArray);
		return getSKUIDDtails(allSKUIDs, accountNumber);
	}

	public static void margeAllSKUIDsInSingleArrayForProduct(ArrayList<String> allSKUIDs, JSONArray SKUs,
			JSONArray sampleSKUs, JSONArray freeGifts) throws Exception {
		allSKUIDs.addAll(JSONArrayToArrayList(SKUs));
		allSKUIDs.addAll(JSONArrayToArrayListForSampleandGifts(sampleSKUs));
		allSKUIDs.addAll(JSONArrayToArrayListForSampleandGifts(freeGifts));
	}

	public static Map<String, DBObject> getSKUIDDtails(ArrayList<String> allSKUIDs, String accountNumber)
			throws JSONException {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		BasicDBObject elemMatch = new BasicDBObject();
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
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		List<DBObject> inventoryDetails = table.find(searchQuery, projection).toArray();
		return mappingItemDetailsWithSKUID(allSKUIDs, inventoryDetails);
	}

	public static Map<String, DBObject> mappingItemDetailsWithSKUID(ArrayList<String> skuIDs,
			List<DBObject> inventoryDetails) {
		Map<String, DBObject> itemDetailsMap = new HashMap<String, DBObject>();
		for (DBObject inventory : inventoryDetails) {
			if (inventory.containsField("lazada")) {
				List<DBObject> channelItemDetailForFilter = (List<DBObject>) inventory.get("lazada");
				DBObject channelItemDetail = channelItemDetailForFilter.get(0);
				if (channelItemDetail.containsField("skuID")) {
					if (skuIDs.contains(channelItemDetail.get("skuID"))) {
						DBObject updateInventory = new BasicDBObject();
						updateInventory.put("SKU", inventory.get("SKU"));
						updateInventory.put("sellerSKU", inventory.get("customSKU"));
						updateInventory.put("SKUID", channelItemDetail.get("skuID"));
						if (inventory.containsField("lazada")) {
							List<DBObject> channelInventoryDetails = (List<DBObject>) inventory.get("lazada");
							DBObject channelInventory = channelInventoryDetails.get(0);
							if (channelInventory.containsField("itemUrl")) {
								updateInventory.put("itemUrl", channelInventory.get("itemUrl"));
							}
							if (inventory.containsField("imageURL")) {
								String imageURL = (String) inventory.get("imageURL");
								if (channelInventory.containsField("imageURI")) {
									List<String> imageURIArray = (List<String>) channelInventory.get("imageURI");
									if (imageURIArray.size() > 0) {
										updateInventory.put("imageURL", imageURL + imageURIArray.get(0));
									}
								}
							}
							if (channelInventory.containsField("itemTitle")) {
								updateInventory.put("itemTitle", channelInventory.get("itemTitle"));
							}
							if (channelInventory.containsField("variantDetails")) {
								updateInventory.put("variantDetails", channelInventory.get("variantDetails"));
							}
							if (channelInventory.containsField("itemAmount")) {
								updateInventory.put("itemOrginalAmount", channelInventory.get("itemAmount"));
							}
						}
						itemDetailsMap.put((String) channelItemDetail.get("skuID"), updateInventory);
					}
				}
			}
		}
		return itemDetailsMap;
	}

	public static void convetToDBStructure(JSONObject flexiComboItems, Map<String, DBObject> itemDetailsSKUIDmap,
			Exchange exchange) throws JSONException {
		BasicDBObject flexiCombo = new BasicDBObject();
		if (flexiComboItems.has("type") && flexiComboItems.getString("type").equalsIgnoreCase("Flexi-combo")) {
			flexiCombo.put("promotionType", PromotionType.FLEXI_COMBO.toString());
		}
		flexiCombo.put("promotionID", flexiComboItems.getString("id"));
		flexiCombo.put("promotionName", flexiComboItems.getString("name"));
		flexiCombo.put("startDate", flexiComboItems.getLong("start_time") / 1000);
		flexiCombo.put("endDate", flexiComboItems.getLong("end_time") / 1000);
		flexiCombo.put("timeCreated", System.currentTimeMillis() / 1000);
		flexiCombo.put("discountApplyFor", flexiComboItems.getString("apply"));
		flexiCombo.put("criteriaType", flexiComboItems.getString("criteria_type"));
		flexiCombo.put("maxOrders", flexiComboItems.getString("order_numbers"));
		flexiCombo.put("status", flexiComboItems.getString("status"));
		if (flexiComboItems.has("discount_type")) {
			flexiCombo.put("discountType", flexiComboItems.getString("discount_type"));
		}
		if (flexiComboItems.has("criteria_value")) {
			flexiCombo.put("criteriaValues", JSONArrayToArrayList(flexiComboItems.getJSONArray("criteria_value")));
		}
		if (flexiComboItems.has("discount_value")) {
			flexiCombo.put("discountValues", JSONArrayToArrayList(flexiComboItems.getJSONArray("discount_value")));
		}
		if (flexiComboItems.has("products")) {
			flexiCombo.put("itemDetails", itemDetailsForSKUID(
					JSONArrayToArrayList(flexiComboItems.getJSONArray("products")), itemDetailsSKUIDmap));
		}
		if (flexiComboItems.has("sample_skus")) {
			flexiCombo.put("sampleSKUs",
					itemDetailsForSKUID(
							JSONArrayToArrayListForSampleandGifts(flexiComboItems.getJSONArray("sample_skus")),
							itemDetailsSKUIDmap));
		}
		if (flexiComboItems.has("gift_skus")) {
			flexiCombo.put("freeGifts",
					itemDetailsForSKUID(
							JSONArrayToArrayListForSampleandGifts(flexiComboItems.getJSONArray("gift_skus")),
							itemDetailsSKUIDmap));
		}
		exchange.setProperty("flexiComboForDB", LazadaUtil.parseToJsonObject(flexiCombo));
	}

	public static List<DBObject> itemDetailsForSKUID(ArrayList<String> SKUIDLists,
			Map<String, DBObject> itemDetailsSKUIDmap) {
		List<DBObject> itemDetailsList = new ArrayList<DBObject>();
		for (String SKUID : SKUIDLists) {
			if (itemDetailsSKUIDmap.containsKey(SKUID)) {
				DBObject itemDetail = itemDetailsSKUIDmap.get(SKUID);
				if (itemDetail != null) {
					itemDetailsList.add(itemDetail);
				}
			}
		}
		return itemDetailsList;
	}

	public static ArrayList<String> JSONArrayToArrayList(JSONArray data) throws JSONException {
		ArrayList<String> listOfData = new ArrayList<String>();
		for (int i = 0; i < data.length(); i++) {
			listOfData.add(data.getString(i));
		}
		return listOfData;
	}

	public static ArrayList<String> JSONArrayToArrayListForSampleandGifts(JSONArray data) throws JSONException {
		ArrayList<String> listOfData = new ArrayList<String>();
		for (int i = 0; i < data.length(); i++) {
			JSONObject dataObject = data.getJSONObject(i);
			if (dataObject.has("sku_id")) {
				listOfData.add(dataObject.getString("sku_id"));
			}
		}
		return listOfData;
	}
}
