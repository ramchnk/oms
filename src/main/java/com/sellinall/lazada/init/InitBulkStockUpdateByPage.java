package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.DBObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.bl.GetProductDetails;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.DateUtil;

public class InitBulkStockUpdateByPage implements Processor {
	static Logger log = Logger.getLogger(InitBulkStockUpdateByPage.class.getName());
	private static final int limit = 50;

	public void process(Exchange exchange) throws Exception {
		boolean isLastPage = true;
		boolean isQuantityDiff = false;
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		int index = exchange.getProperty("CamelLoopIndex", Integer.class);
		ArrayList<DBObject> inventoryDetails = (ArrayList<DBObject>) exchange.getProperty("inventoryList");
		Map<String, DBObject> inventoryDataMap = new LinkedHashMap<String, DBObject>();
		Map<String, List<String>> itemIDMap = new LinkedHashMap<String, List<String>>();
		if (inventoryDetails.size() > 0) {
			int noOfPage = (inventoryDetails.size() / limit) + (inventoryDetails.size() % limit > 0 ? 1 : 0);
			int fromIndex = index * limit;
			int toIndex = fromIndex + limit;
			if (noOfPage == (index + 1)) {
				toIndex = inventoryDetails.size();
			} else {
				isLastPage = false;
			}

			String countryCode = exchange.getProperty("countryCode", String.class);
			long promotionStartDate = DateUtil.getUnixTimestamp(Config.getConfig().getPromotionStartDate(),
					"dd-MM-yyyy HH:mm:ss", LazadaUtil.timeZoneCountryMap.get(countryCode));
			long promotionEndDate = DateUtil.getUnixTimestamp(Config.getConfig().getPromotionEndDate(),
					"dd-MM-yyyy HH:mm:ss", LazadaUtil.timeZoneCountryMap.get(countryCode));
			long currentTime = System.currentTimeMillis() / 1000L;

			if (promotionStartDate < currentTime && currentTime < promotionEndDate) {
				isQuantityDiff = true;
			}

			List<DBObject> inventoryVariantDetails = inventoryDetails.subList(fromIndex, toIndex);
			JSONArray processCustomSKUs = new JSONArray();
			for (DBObject dbObject : inventoryVariantDetails) {
				String itemId = dbObject.get("itemID").toString();
				String customSKU = dbObject.get("customSKU").toString();
				processCustomSKUs.put(customSKU);
				if (itemIDMap.containsKey(itemId)) {
					List<String> oldCustomSKUs = itemIDMap.get(itemId);
					oldCustomSKUs.add(customSKU);
					itemIDMap.put(itemId, oldCustomSKUs);
				} else {
					List<String> newCustomSKUs = new ArrayList<String>();
					newCustomSKUs.add(customSKU);
					itemIDMap.put(itemId, newCustomSKUs);
				}
				inventoryDataMap.put(customSKU, dbObject);
			}

			if (isQuantityDiff) {
				for (Map.Entry<String, List<String>> entry : itemIDMap.entrySet()) {
					String itemID = entry.getKey();
					List<String> customSKUList = entry.getValue();
					String response = GetProductDetails.getProductItems(exchange, itemID, "");
					processProductItemDetails(response, accountNumber, nickNameID, itemID, inventoryDataMap);
				}
			}

			exchange.setProperty("itemIDMap", itemIDMap);
			exchange.setProperty("inventoryDataMap", inventoryDataMap);
			exchange.setProperty("processCustomSKUs", processCustomSKUs);
			exchange.setProperty("list", inventoryDataMap.values());
		}
		exchange.setProperty("isLastPage", isLastPage);
		exchange.setProperty("isQuantityDiff", isQuantityDiff);
		exchange.setProperty("isUpdateSellableStock", true);
	}

	private void processProductItemDetails(String response, String accountNumber, String nickNameID,
			String itemID, Map<String, DBObject> inventoryDataMap) throws JSONException {
		if (response == null) {
			log.error("Invalid response for get item details for accountNumber : " + accountNumber + " ,for nickNameID: "
					+ nickNameID);
			return;
		}
		if (response.isEmpty()) {
			log.error("Empty response for get item details for accountNumber : " + accountNumber + " ,for nickNameID: "
					+ nickNameID);
			return;
		}
		JSONObject channelResponse = new JSONObject(response);
		if (channelResponse.has("data")) {
			JSONObject dataObject = channelResponse.getJSONObject("data");
			if (dataObject.has("skus")) {
				JSONArray skus = dataObject.getJSONArray("skus");
				for (int i = 0; i < skus.length(); i++) {
					JSONObject skusObj = skus.getJSONObject(i);
					String sellerSKU = skusObj.getString("SellerSku");
					if (inventoryDataMap.containsKey(sellerSKU)) {
						DBObject invDBObject = inventoryDataMap.get(sellerSKU);
						int overAllQuantity = (int) invDBObject.get("overAllQuantity");
						invDBObject.put("overAllQuantity", overAllQuantity - skusObj.getInt("quantity"));
					}
				}
			} else {
				log.error("SKUs missing in response for get item details " + response + ", for accountNumber : "
						+ accountNumber + " for nickNameID: " + nickNameID + " & itemID: " + itemID);
			}
		} else {
			log.error("Response for get item details " + response + ", for accountNumber : " + accountNumber
					+ " for nickNameID: " + nickNameID + " & itemID: " + itemID);
		}
	}
}