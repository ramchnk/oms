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

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.enums.SIAInventoryStatus;

public class InitUpdateStatusRequest implements Processor {
	static Logger log = Logger.getLogger(InitUpdateStatusRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		DBObject skuInvObject = exchange.getIn().getBody(DBObject.class);
		String mainSKU = (String) skuInvObject.get("_id");
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		boolean isChildVariantStatusUpdate = false;
		boolean isPartialAutoStatusUpdate = true;
		if (exchange.getProperties().containsKey("isPartialAutoStatusUpdate")) {
			isPartialAutoStatusUpdate = exchange.getProperty("isPartialAutoStatusUpdate", Boolean.class);
		}

		List<DBObject> skuInvList = (List<DBObject>) skuInvObject.get("documents");
		Map<String, JSONObject> sellerSKUFeedMap = exchange.getProperty("sellerSKUFeedMap", Map.class);
		List<String> invCustomSKUS = new ArrayList();
		Map<String, JSONObject> skuDataMap = new LinkedHashMap<String, JSONObject>();
		List<String> inactiveCustomSKUList = new ArrayList<String>();
		List<String> activeCustomSKUList = new ArrayList<String>();

		String itemID = "";
		for (DBObject skuInv : skuInvList) {
			if (skuInv.containsKey("itemID")) {
				itemID = skuInv.get("itemID").toString();
			}
			String customSKU = skuInv.get("customSKU").toString();
			String SKU = skuInv.get("SKU").toString();
			if (SKU.contains("-")) {
				exchange.setProperty("parentSKU", mainSKU);
				isChildVariantStatusUpdate = true;
			}
			JSONObject sukObj = sellerSKUFeedMap.get(customSKU);
			sukObj.put("SKU", SKU);
			if (sukObj.has("updateToStatus")) {
				if (sukObj.getString("updateToStatus").equalsIgnoreCase("inactive")) {
					inactiveCustomSKUList.add(customSKU);
				} else if (sukObj.getString("updateToStatus").equalsIgnoreCase("active")) {
					activeCustomSKUList.add(customSKU);
				}
			}
			skuDataMap.put(skuInv.get("customSKU").toString(), LazadaUtil.parseToJsonObject(skuInv));
			invCustomSKUS.add(skuInv.get("customSKU").toString());
		}

		boolean isEligibleToDeactiveProduct = false;
		if (!isPartialAutoStatusUpdate && inactiveCustomSKUList.size() > 0) {
			boolean isActiveSKUFound = checkForActiveProducts(exchange, mainSKU, activeCustomSKUList,
					inactiveCustomSKUList, nickNameID);
			if (isActiveSKUFound) {
				// removing inactive sku from map
				for (int i = 0; i < inactiveCustomSKUList.size(); i++) {
					skuDataMap.remove(inactiveCustomSKUList.get(i));
				}
			} else {
				isEligibleToDeactiveProduct = true;
			}
		}
		if (!isPartialAutoStatusUpdate && skuDataMap.isEmpty()) {
			// for partial auto status update, if any child came for inactive status update,
			// then we will skip that specific sku & update as success status in sheet
			for (int i = 0; i < inactiveCustomSKUList.size(); i++) {
				if (sellerSKUFeedMap.containsKey(inactiveCustomSKUList.get(i))) {
					JSONObject feedMessage = sellerSKUFeedMap.get(inactiveCustomSKUList.get(i));
					feedMessage.put("status", "success");
					/*
					 * if any active child found, then we will skip inactive request & update
					 * inactive request back to active status
					 */
					revertInventoryStatus(feedMessage);
				}
			}
		}

		exchange.setProperty("SKU", mainSKU);
		exchange.setProperty("isEligibleToDeactiveProduct", isEligibleToDeactiveProduct);
		exchange.setProperty("skuDataMap", skuDataMap);
		exchange.setProperty("invCustomSKUS", invCustomSKUS);
		exchange.setProperty("hasChildVariants", isChildVariantStatusUpdate);
		if (itemID.isEmpty()) {
			LazadaUtil.loadItemIDFromDB(exchange);
		} else {
			exchange.setProperty("itemID", itemID);
		}
	}

	private void revertInventoryStatus(JSONObject feedMessage) throws JSONException {
		DBObject query = new BasicDBObject();
		query.put("accountNumber", feedMessage.getString("accountNumber"));
		query.put("SKU", feedMessage.getString("SKU"));
		query.put("lazada.nickNameID", feedMessage.getString("nickNameID"));

		DBObject setObj = new BasicDBObject();
		if (feedMessage.getString("updateToStatus").equals("inactive")) {
			setObj.put("lazada.$.status", SIAInventoryStatus.ACTIVE.toString());
		} else {
			setObj.put("lazada.$.status", SIAInventoryStatus.INACTIVE.toString());
		}

		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		table.update(query, new BasicDBObject("$set", setObj));
	}

	private boolean checkForActiveProducts(Exchange exchange, String mainSKU, List<String> activeCustomSKUList,
			List<String> inactiveCustomSKUList, String nickNameID) {
		DBObject query = new BasicDBObject();
		query.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		query.put("SKU", new BasicDBObject("$regex", mainSKU));

		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		List<DBObject> inventoryList = table.find(query).toArray();

		JSONArray productCustomSKUList = new JSONArray();
		boolean isActiveSKUFound = false;
		for (DBObject inventory : inventoryList) {
			if (inventoryList.size() > 1 && !inventory.get("SKU").toString().contains("-")) {
				// skipping variant parent only
				continue;
			}
			String customSKU = inventory.get("customSKU").toString();
			productCustomSKUList.put(customSKU);
			List<DBObject> channelArray = (List<DBObject>) inventory.get("lazada");
			DBObject channelObj = getChannelObj(channelArray, nickNameID);
			if (channelObj != null) {
				String status = channelObj.get("status").toString();
				int quantity = (int) channelObj.get("noOfItem");
				if (status.equals(SIAInventoryStatus.ACTIVE.toString()) && quantity > 0
						&& !inactiveCustomSKUList.contains(customSKU)) {
					isActiveSKUFound = true;
				} else if (status.equals(SIAInventoryStatus.INACTIVE.toString())
						&& activeCustomSKUList.contains(customSKU)) {
					isActiveSKUFound = true;
				}
			}
		}
		exchange.setProperty("productCustomSKUList", productCustomSKUList);
		return isActiveSKUFound;
	}

	private DBObject getChannelObj(List<DBObject> channelArray, String nickNameID) {
		for (DBObject channelObj : channelArray) {
			if (channelObj.get("nickNameID").toString().equals(nickNameID)) {
				return channelObj;
			}
		}
		return null;
	}

}
