package com.sellinall.lazada.init;

import java.io.IOException;
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

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;
import com.sellinall.util.InventorySequence;
import com.sellinall.util.enums.SIAInventoryStatus;

public class InitializeInsertIndividualSkuModelInvetoryItem implements Processor {
	static Logger log = Logger.getLogger(InitializeInsertIndividualSkuModelInvetoryItem.class.getName());

	public void process(Exchange exchange) throws Exception {
		boolean isInventoryEmpty = exchange.getProperty("isInventoryEmpty", Boolean.class);
		String merchantID = exchange.getProperty("merchantID", String.class);
		JSONObject item = exchange.getProperty("item", JSONObject.class);
		JSONArray SKUs = new JSONArray();
		if (item.has("skus")) {
			SKUs = item.getJSONArray("skus");
		} else {
			SKUs = item.getJSONArray("Skus");
		}
		exchange.removeProperty("itemImages");
		if (item.has("images")) {
			JSONArray itemImages = new JSONArray(item.getString("images"));
			if (itemImages.length() > 0) {
				exchange.setProperty("itemImages", itemImages);
			}
		}
		exchange.setProperty("SKUS", processChildRecordExistingOrder(exchange, SKUs, item));
		String category = "";
		if (item.has("primary_category")) {
			category = item.getString("primary_category");
		} else {
			category = item.getString("PrimaryCategory");
		}
		JSONObject lookupResponse = getCategoryResponse(exchange, category);
		if (SKUs.length() > 1) {
			exchange.setProperty("itemHasVariants", true);
			exchange.setProperty("SIAVariantsDetails", LazadaUtil.getVariantsFromCategory(lookupResponse));
		} else {
			exchange.setProperty("itemHasVariants", false);
		}
		JSONArray attributes = LazadaUtil.getCategoryAttributes(lookupResponse);
		exchange.setProperty("attributeAndTypeMap", LazadaUtil.constructAttributesAndAttributeTypeMap(attributes));
		boolean isAutoMatchFound = false;
		if (exchange.getProperties().containsKey("itemIsExistInInventoryDetails")) {
			isAutoMatchFound = exchange.getProperty("itemIsExistInInventoryDetails", Boolean.class);
		}
		if (isAutoMatchFound) {
			exchange.setProperty("SKU", exchange.getProperty("linkToSKU", String.class));
			exchange.setProperty("isNewItem", false);
		} else {
			exchange.setProperty("isNewItem", true);
		}
		exchange.setProperty("isEligibleToInsertLinked", false);
		if (isInventoryEmpty || !isAutoMatchFound) {
			exchange.setProperty("isEligibleToInsertLinked", true);
			exchange.setProperty("SKU", InventorySequence.getNextSKU(merchantID));
			if (exchange.getProperties().containsKey("itemStatus")) {
				if (exchange.getProperty("itemStatus", String.class).equals("active")) {
					exchange.setProperty("status", SIAInventoryStatus.ACTIVE.toString());
				}
			} else {
				exchange.setProperty("status", SIAInventoryStatus.INACTIVE.toString());
			}
		}
	}

	private static JSONObject getCategoryResponse(Exchange exchange, String categoryID) throws JSONException {
		Map<String, String> header = new HashMap<String, String>();
		header.put("Content-Type", "application/json");
		header.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		header.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		String totalURL = Config.getConfig().getSIAListingLookupServerURL() + "/lazada/category/"
				+ exchange.getProperty("countryCode", String.class) + "/" + categoryID;
		log.info(totalURL + ", for account : " + exchange.getProperty("accountNumber"));
		try {
			JSONObject lookupJSON = HttpsURLConnectionUtil.doGet(totalURL, header);
			lookupJSON = new JSONObject(lookupJSON.getString("payload"));
			return lookupJSON;
		} catch (Exception exception) {
			log.error("Failed to get category attributes for categoryID : " + categoryID + ", refrenceID : "
					+ exchange.getProperty("refrenceID", String.class) + ", for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class));
		}
		return new JSONObject();
	}
	
	private static JSONArray processChildRecordExistingOrder(Exchange exchange, JSONArray SKUs, JSONObject item)
			throws JSONException {
		JSONArray SKUsArray = new JSONArray();
		if (exchange.getProperties().containsKey("refrenceIDAndSKUFromDB")
				&& exchange.getProperty("refrenceIDAndSKUFromDB", Map.class).size() > 0) {
			Map<String, Integer> refrenceIDAndSKUFromDB = exchange.getProperty("refrenceIDAndSKUFromDB", Map.class);
			Map<Integer, JSONObject> oldSKUs = new HashMap<Integer, JSONObject>();
			List<JSONObject> newSKUs = new ArrayList<JSONObject>();
			for (int i = 0; i < SKUs.length(); i++) {
				JSONObject skuObj = SKUs.getJSONObject(i);
				if (refrenceIDAndSKUFromDB.containsKey(skuObj.get("SellerSku"))) {
					oldSKUs.put(refrenceIDAndSKUFromDB.get(skuObj.get("SellerSku")), skuObj);
				} else {
					newSKUs.add(skuObj);
				}
			}
			List<JSONObject> finalSKUList = new ArrayList(oldSKUs.values());
			finalSKUList.addAll(newSKUs);
			SKUsArray = new JSONArray(finalSKUList.toString());
		} else {
			for (int i = 0; i < SKUs.length(); i++) {
				SKUsArray.put(SKUs.get(i));
			}
		}
		if (item.has("skus")) {
			item.put("skus", SKUsArray);
		} else if (item.has("Skus")) {
			item.put("Skus", SKUsArray);
		}
		return SKUsArray;
	}
}
