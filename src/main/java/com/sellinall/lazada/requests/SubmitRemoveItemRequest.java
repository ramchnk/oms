package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class SubmitRemoveItemRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitRemoveItemRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accountNumber = "";
		if (exchange.getProperties().containsKey("accountNumber")) {
			accountNumber = (String) exchange.getProperty("accountNumber");
		}
		BasicDBObject inventory = exchange.getProperty("inventory", BasicDBObject.class);
		BasicDBObject channel = (BasicDBObject) inventory.get(exchange.getProperty("channelName", String.class));
		String accessToken = exchange.getProperty("accessToken", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		JSONArray skuList = new JSONArray();
		JSONArray sellerSkuList = new JSONArray();
		String response = null;
		Map<String,String> skuIdAndItemIdMap = new HashMap<String,String>();
		if (inventory.containsField("variants")) {
			List<DBObject> inventoryList = (List<DBObject>) exchange.getProperty("inventoryList");
			skuList = getVariantsRefrenceIDList(accountNumber, inventoryList, skuIdAndItemIdMap);
			getVariantsSellerSKUList(accountNumber, inventoryList, sellerSkuList);
		} else {
			if (channel.containsField("itemID") && channel.containsField("skuID")) {
				skuIdAndItemIdMap.put(channel.getString("skuID"), channel.getString("itemID"));
			} else if (channel.containsField("refrenceID")) {
				skuList.put(channel.getString("refrenceID"));
			} else {
				log.error("Either itemID/skuID or refrenceID not found for customSKU: " + channel.getString("customSKU")
						+ " and for nickNameID: " + channel.getString("nickNameID") + " for accountNumber: "
						+ accountNumber);
			}
			if (inventory.containsField("customSKU")) {
				sellerSkuList.put(inventory.getString("customSKU"));
			}
		}
		String url = exchange.getProperty("hostURL", String.class);
		map.put("access_token", accessToken);
		if (skuIdAndItemIdMap.entrySet().size() > 0) {
			JSONArray modifiedSKUList = getSkuIDAndItemIDPayload(skuIdAndItemIdMap);
			mergeJSONArray(modifiedSKUList, skuList);
		}
		String param = "&seller_sku_list=" + URLEncoder.encode(skuList.toString());
		map.put("seller_sku_list", skuList.toString());
		exchange.setProperty("isItemDeletedFromMarketPlace", false);
		try {
			String clientID = Config.getConfig().getLazadaClientID();
			String clientSecret = Config.getConfig().getLazadaClientSecret();
			response = NewLazadaConnectionUtil.callAPI(url, "/product/remove", accessToken, map, "", param,
					"POST", clientID, clientSecret);
			JSONObject responseFromChannel = new JSONObject(response);
			if (responseFromChannel.has("code") && responseFromChannel.getString("code").equals("0")) {
				exchange.setProperty("isItemDeletedFromMarketPlace", true);
				exchange.setProperty("customSKUListToPullProductMaster", sellerSkuList);
				return;
			} else if(responseFromChannel.has("detail")){
				JSONArray errorDetails = responseFromChannel.getJSONArray("detail");
				Set<String> errorMessageSet = new HashSet<String>();
				for (int i = 0; i < errorDetails.length(); i++) {
					JSONObject error = errorDetails.getJSONObject(i);
					if (error.has("message")) {
						errorMessageSet.add(error.getString("message"));
					}
				}
				// already market side delete item remove from our system
				if (errorMessageSet.size() == 1 && (errorMessageSet.iterator().next().equals("SELLER_SKU_NOT_FOUND")
						|| errorMessageSet.iterator().next().equals("ITEM_NOT_FOUND"))) {
					exchange.setProperty("isItemDeletedFromMarketPlace", true);
					exchange.setProperty("customSKUListToPullProductMaster", sellerSkuList);
					return;
				}
			} else if (exchange.getProperty("isRequestFromBatch", Boolean.class)) {
				if (responseFromChannel.has("message") && !responseFromChannel.getString("message").isEmpty()) {
					exchange.setProperty("failureReason", responseFromChannel.getString("message"));
				}
			}
			log.error("Response From Channel = " + response + ", for account : " + accountNumber);
		} catch (Exception e) {
			log.error("Exception occured while removing SKUList: " + skuList.toString() + " ,accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response : " + response);
			e.printStackTrace();
		}
	}

	private JSONArray getSkuIDAndItemIDPayload(Map<String, String> skuIdAndItemIdMap) {
		JSONArray modifiedSKUList = new JSONArray();
		for (Entry<String, String> entry : skuIdAndItemIdMap.entrySet()) {
			String skuID = entry.getKey();
			String itemID = entry.getValue();
			String payload = "SkuId_" + itemID + "_" + skuID;
			modifiedSKUList.put(payload);
		}
		return modifiedSKUList;
	}

	private void mergeJSONArray(JSONArray sourceArray, JSONArray destinationArray) throws JSONException {
		for (int i = 0; i < sourceArray.length(); i++) {
			destinationArray.put(sourceArray.getString(i));
		}
	}

	private void getVariantsSellerSKUList(String accountNumber, List<DBObject> inventoryList, JSONArray sellerSkuList)
			throws JSONException {
		for (DBObject inventoryObj : inventoryList) {
			List<BasicDBObject> lazada = (ArrayList<BasicDBObject>) inventoryObj.get("lazada");
			BasicDBObject lazadaObject = lazada.get(0);
			if (inventoryObj.containsField("customSKU")) {
				sellerSkuList.put(inventoryObj.get("customSKU"));
			} else {
				log.error("customSKU not found in variant for nickNameID: " + inventoryObj.get("nickNameID")
						+ " for accountNumber: " + accountNumber);
			}
		}
	}

	private JSONArray getVariantsRefrenceIDList(String accountNumber, List<DBObject> inventoryList,
			Map<String, String> skuIdAndItemIdMap) throws JSONException {
		JSONArray refrenceIDList = new JSONArray();
		for (DBObject inventoryObj : inventoryList) {
			List<BasicDBObject> lazada = (ArrayList<BasicDBObject>) inventoryObj.get("lazada");
			BasicDBObject lazadaObject = lazada.get(0);
			if (lazadaObject.containsField("itemID") && lazadaObject.containsField("skuID")) {
				skuIdAndItemIdMap.put(lazadaObject.getString("skuID"), lazadaObject.getString("itemID"));
			} else if (lazadaObject.containsField("refrenceID")) {
				refrenceIDList.put(lazadaObject.getString("refrenceID"));
			} else {
				log.error("Either itemID/skuID or refrenceID not found in variant for customSKU: "
						+ lazadaObject.getString("customSKU") + " and for nickNameID: "
						+ lazadaObject.getString("nickNameID") + " for accountNumber: " + accountNumber);
			}
		}
		return refrenceIDList;
	}
}