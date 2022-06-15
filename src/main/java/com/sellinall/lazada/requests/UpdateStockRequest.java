package com.sellinall.lazada.requests;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;
import com.sellinall.util.enums.Actor;
import com.sellinall.util.enums.StockEventType;

public class UpdateStockRequest implements Processor {
	static Logger log = Logger.getLogger(UpdateStockRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		String testingAccountNumber = Config.getConfig().getTestAccountNumber();
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		if(accountNumber.equals(testingAccountNumber)) {
			int index = exchange.getProperty("CamelLoopIndex", Integer.class);
			Map<String, JSONObject> sellerSKUFeedMap = exchange.getProperty("sellerSKUFeedMap", Map.class);
			List<String> customSKUs = (List<String>) exchange.getProperty("customSKUs");
			String customSKU = customSKUs.get(index);
			JSONObject skuObject = new JSONObject();
			if (sellerSKUFeedMap.containsKey(customSKU)) {
				skuObject = sellerSKUFeedMap.get(customSKU);
			}
			skuObject.put("needToUpdateStatus", false);
			if (skuObject.has("needToUpdateStock") && skuObject.getBoolean("needToUpdateStock")) {
				String sellerSKU = skuObject.getString("customSKU");
				String SKU = skuObject.getString("SKU");
				skuObject.put("sellerSKU", skuObject.getString("customSKU"));
				JSONObject response = LazadaUtil.getListingQuantities(accountNumber, nickNameID, sellerSKU, SKU);
				if (response != null) {
					JSONArray listing = response.getJSONArray("listing");
					if (listing.length() == 0) {
						log.error("Listing array is empty in quantities API reposne for accountNumber: " + accountNumber
								+ ", nickNameID: " + nickNameID + ", sellerSKU: " + sellerSKU + ", response: " + response);
					}
					BasicDBList updateDBList = new BasicDBList();
					for (int i = 0; i < listing.length(); i++) {
						BasicDBObject listingBody = BasicDBObject.parse(listing.get(i).toString());
						listingBody.put("needToUpdateStatus", false);
						listingBody.put("accountNumber", skuObject.getString("accountNumber"));
						listingBody.put("nickNameID", skuObject.getString("nickNameID"));
						updateDBList.add(listingBody);
					}
					LazadaUtil.updateStockFeeds(accountNumber, nickNameID, updateDBList);
				}
			}
		} else {
			JSONArray customSKUList = new JSONArray();
			if (exchange.getProperties().containsKey("isBulkStatusUpdate")
					&& exchange.getProperty("isBulkStatusUpdate", Boolean.class)) {
				JSONObject SKUMap = exchange.getProperty("SKUMap", JSONObject.class);
				customSKUList.put(SKUMap.getString("customSKU"));
			} else {
				customSKUList = (JSONArray) exchange.getProperty("customSKUList");
			}

			JSONArray list = new JSONArray();
			for (int i = 0; i < customSKUList.length(); i++) {
				String sku = customSKUList.getString(i);
				JSONObject sellerSku = new JSONObject();
				sellerSku.put("sellerSKU", sku);
				list.put(sellerSku);
			}

			JSONObject payload = new JSONObject();
			payload.put("actor", Actor.SYSTEM.toString());
			payload.put("stockEventType", StockEventType.LISTING_ACTIVATED.toString());
			payload.put("list", list);

			Map<String, String> header = new HashMap<String, String>();
			header.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
			header.put("Content-Type", "application/json");
			header.put("accountNumber", accountNumber);

			String url = Config.getConfig().getInventoryUrl() + "/listing/quantities/" + nickNameID;
			try {
				JSONObject response = HttpsURLConnectionUtil.doPut(url, payload.toString(), header);
				if (response.getInt("httpCode") == HttpStatus.SC_OK) {
					log.info("Quantity update message published payload is: " + payload.toString());
				} else {
					log.error("Failed Quantity update message published payload is : " + payload.toString()
							+ " And response is: " + response.toString());
				}
			} catch (IOException e) {
				e.printStackTrace();

			}
		}
	}
}
