package com.sellinall.lazada.db;

import java.util.ArrayList;
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
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.enums.PromotionType;

public class LoadInventoryBySKUs implements Processor {
	static Logger log = Logger.getLogger(LoadInventoryBySKUs.class.getName());

	public void process(Exchange exchange) throws Exception {
		List<String> listOfSKUS = exchange.getProperty("listOfSKUS", ArrayList.class);
		String action = "";
		if (exchange.getProperties().containsKey("action")) {
			action = exchange.getProperty("action", String.class);
		}
		loadInventoryBySKUS(exchange, listOfSKUS, action);
	}

	private void loadInventoryBySKUS(Exchange exchange, List<String> listOfSKUS, String action) throws JSONException {
		String promotionType = "";
		if (exchange.getProperties().containsKey("promotionType")) {
			promotionType = exchange.getProperty("promotionType", String.class);
		}
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String channel = nickNameID.split("-")[0];
		BasicDBObject dbquery = new BasicDBObject();
		dbquery.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		dbquery.put(channel + ".nickNameID", exchange.getProperty("nickNameID", String.class));
		dbquery.put("SKU", new BasicDBObject("$in", listOfSKUS));
		BasicDBObject projection = new BasicDBObject();
		projection.put("_id", 0);
		projection.put("SKU", 1);
		projection.put(channel + ".itemID", 1);
		projection.put(channel + ".skuID", 1);
		projection.put(channel + ".nickNameID", 1);
		List<DBObject> inventoryList = getInventories(dbquery, projection);
		if (action.equalsIgnoreCase("updatePromotion") || promotionType.equals(PromotionType.VOUCHER.toString())
				|| promotionType.equals(PromotionType.FREE_SHIPPING.toString())) {
			processInventoryForAddPromotionItem(exchange, inventoryList, nickNameID);
		} else {
			processInventoryList(exchange, inventoryList, nickNameID);
		}
	}

	private List<DBObject> getInventories(DBObject dbquery, DBObject projection) {
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		return table.find(dbquery, projection).toArray();
	}

	private void processInventoryForAddPromotionItem(Exchange exchange, List<DBObject> inventoryList, String nickNameID)
			throws JSONException {
		Map<String, String> SKUIDMap = new HashMap<String, String>();
		JSONArray skusList = new JSONArray();
		for (DBObject invObj : inventoryList) {
			JSONObject inventoryObj = LazadaUtil.parseToJsonObject(invObj);
			String SKU = inventoryObj.getString("SKU");
			JSONArray channelList = inventoryObj.getJSONArray("lazada");
			for (int i = 0; i < channelList.length(); i++) {
				JSONObject channelObject = channelList.getJSONObject(i);
				if (channelObject.has("nickNameID") && channelObject.getString("nickNameID").equals(nickNameID)) {
					String skuID = channelObject.getString("skuID");
					skusList.put(skuID);
					SKUIDMap.put(skuID, SKU);
					break;
				}
			}
		}
		exchange.setProperty("skusList", skusList);
		exchange.setProperty("SKUIDMap", SKUIDMap);
	}

	private void processInventoryList(Exchange exchange, List<DBObject> inventoryList, String nickNameID) throws JSONException {
		List<String> samples = exchange.getProperty("samples", ArrayList.class);
		List<String> gifts = exchange.getProperty("gifts", ArrayList.class);
		List<String> skus = exchange.getProperty("skus", ArrayList.class);
		JSONArray sampleSKUList = new JSONArray();
		JSONArray freeGiftList = new JSONArray();
		JSONArray skusList = new JSONArray();
		for (DBObject invObj : inventoryList) {
			JSONObject inventoryObj = LazadaUtil.parseToJsonObject(invObj);
			String SKU = inventoryObj.getString("SKU");
			JSONArray channelList = inventoryObj.getJSONArray("lazada");
			JSONObject channelObj = new JSONObject();
			for (int i = 0; i < channelList.length(); i++) {
				JSONObject channelObj1 = channelList.getJSONObject(i);
				if (channelObj1.has("nickNameID") && channelObj1.getString("nickNameID").equals(nickNameID)) {
					channelObj = channelObj1;
					break;
				}
			}
			addSKUsInList(samples, SKU, channelObj, sampleSKUList, null);
			addSKUsInList(gifts, SKU, channelObj, freeGiftList, null);
			addSKUsInList(skus, SKU, channelObj, null, skusList);
		}
		exchange.setProperty("sampleSKUList", sampleSKUList);
		exchange.setProperty("freeGiftList", freeGiftList);
		exchange.setProperty("skusList", skusList);
	}

	private void addSKUsInList(List<String> sourceCheck, String SKU, JSONObject source, JSONArray target, JSONArray skusList) {

		try {
			if (sourceCheck.contains(SKU)) {
				if (skusList != null) {
					skusList.put(source.getString("skuID"));
				} else {
					JSONObject obj = new JSONObject();
					obj.put("productId", source.getString("itemID"));
					obj.put("skuId", source.getString("skuID"));
					target.put(obj);
				}
			}
		} catch (Exception e) {
			log.error("Exception occurred while processing this SKU ; "+ SKU, e);
		}
	}
}