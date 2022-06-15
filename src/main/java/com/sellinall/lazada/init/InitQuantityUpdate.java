package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.config.APIUrlConfig;
import com.mudra.sellinall.config.Config;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.DateUtil;
import com.sellinall.util.enums.SIAInventoryStatus;

public class InitQuantityUpdate implements Processor {
	static Logger log = Logger.getLogger(InitQuantityUpdate.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		String SKU = inBody.getString("SKU");
		boolean isChildVariantStatusUpdate = false;
		if (SKU.contains("-")) {
			isChildVariantStatusUpdate = true;
		}
		exchange.setProperty("isChildVariantStatusUpdate", isChildVariantStatusUpdate);
		String sellerSKU = inBody.getString("sellerSKU");
		exchange.setProperty("sellerSKU", sellerSKU);
		boolean isUpdateSellableStock = false, isUpdateQuantityByDiff = false;
		if (Config.getConfig().isUpdateStockViaSellableQuantityApi()) {
			isUpdateSellableStock = true;
		}
		String countryCode = exchange.getProperty("countryCode", String.class);
		if (exchange.getProperty("isGlobalAccount", Boolean.class)) {
			DBObject inventory = LazadaUtil.getSKUDetails(SKU);
			List<DBObject> lazadaArray = (List<DBObject>) inventory.get("lazada");
			DBObject lazada = lazadaArray.get(0);

			DBObject itemAmount = (DBObject) lazada.get("itemAmount");
			String currencyCode = itemAmount.get("currencyCode").toString();
			countryCode = LazadaUtil.currencyToCountryCodeMap.get(currencyCode);

			exchange.setProperty("countryCode", countryCode);
			exchange.setProperty("currencyCode", currencyCode);
			exchange.setProperty("hostURL", APIUrlConfig.getNewAPIUrl(countryCode.toUpperCase()));
		}
		String promotionStartDateStr = Config.getConfig().getPromotionStartDate();
		String promotionEndDateStr = Config.getConfig().getPromotionEndDate();
		long promotionStartDate = DateUtil.getUnixTimestamp(promotionStartDateStr, "dd-MM-yyyy HH:mm:ss",
				LazadaUtil.timeZoneCountryMap.get(countryCode));
		long promotionEndDate = DateUtil.getUnixTimestamp(promotionEndDateStr, "dd-MM-yyyy HH:mm:ss",
				LazadaUtil.timeZoneCountryMap.get(countryCode));
		long currentTime = System.currentTimeMillis() / 1000L;

		String itemID = "";
		if (inBody.has("itemID")) {
			exchange.setProperty("itemID", inBody.getString("itemID"));
			itemID = inBody.getString("itemID");
		}
		if (inBody.has("skuID")) {
			exchange.setProperty("skuID", inBody.getString("skuID"));
		}
		String status = inBody.getString("status");
		exchange.setProperty("itemCurrentStatus", status);
		String refrenceID = "";
		if (inBody.has("refrenceID")) {
			refrenceID = inBody.getString("refrenceID");
		}
		int totalQuantity = inBody.getInt("totalQuantity");
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		ArrayList<String> warehouseIDList = new ArrayList<String>();
		if (exchange.getProperties().containsKey("warehouseIDList")) {
			warehouseIDList = (ArrayList<String>) exchange.getProperty("warehouseIDList");
		}
		int qtyUnderProcessing = 0, sellableStockFromLazada = 0;
		if (isUpdateSellableStock && promotionStartDate < currentTime && currentTime < promotionEndDate) {
			isUpdateQuantityByDiff = true;
		}
		if ((!isUpdateSellableStock || isUpdateQuantityByDiff)
				&& !status.equals(SIAInventoryStatus.NOT_LISTED.toString())) {
			JSONObject itemResponse = LazadaUtil.getItemDetailsBySellerSKU(exchange);
			if (itemResponse.has("multiWarehouseInventories")
					&& itemResponse.getJSONArray("multiWarehouseInventories").length() == 1) {
				JSONObject stockDetails = itemResponse.getJSONArray("multiWarehouseInventories").getJSONObject(0);
				if (isUpdateQuantityByDiff) {
					if (stockDetails.has("sellableQuantity")) {
						sellableStockFromLazada = stockDetails.getInt("sellableQuantity");
					} else {
						sellableStockFromLazada = stockDetails.getInt("quantity");
					}
				} else {
					if (stockDetails.has("occupyQuantity")) {
						qtyUnderProcessing = stockDetails.getInt("occupyQuantity");
					}
					if (stockDetails.has("withholdQuantity")) {
						qtyUnderProcessing += stockDetails.getInt("withholdQuantity");
					}
				}
			} else {
				if (itemResponse.has("multiWarehouseInventories")
						&& itemResponse.getJSONArray("multiWarehouseInventories").length() > 1) {
					log.warn("Multiple warehouse found for accountNumber:" + accountNumber + ",for nickNameID:" + nickNameID
							+ ",for sellerSKU:" + sellerSKU + ",for itemID:" + itemID + ",SKU:" + SKU
							+ " and warehouse list is :" + itemResponse.get("multiWarehouseInventories"));
				}
				/*
				 * Note : If error response in get product api or found multiple warehouse, then
				 * we will not call adjust sellable quantity api
				 */
				isUpdateQuantityByDiff = false;
				qtyUnderProcessing = LazadaUtil.loadSoldCount(accountNumber, sellerSKU, nickNameID);
			}
		}
		if (inBody.has("maxQuantity")) {
			exchange.setProperty("maxQuantity", inBody.getInt("maxQuantity"));
		}
		JSONArray listingQuantities = inBody.getJSONArray("listingQuantity");
		exchange.setProperty("listingQuantities", listingQuantities);
		boolean isBufferExists = false;
		int bufferQuantity = 0;
		for (int i = 0; i < listingQuantities.length(); i++) {
			JSONObject quantityObject = (JSONObject) listingQuantities.get(i);
			if (quantityObject.has("bufferQuantity")) {
				if (warehouseIDList.contains(quantityObject.get("warehouseID"))) {
					bufferQuantity += quantityObject.getInt("bufferQuantity");
					isBufferExists = true;
					exchange.setProperty("bufferType", quantityObject.getString("bufferType"));
				}
			}
		}
		if (isBufferExists) {
			exchange.setProperty("bufferQuantity", bufferQuantity);
		}
		int quantity = (totalQuantity < 0) ? 0 : totalQuantity;
		int overAllQuantity = quantity + qtyUnderProcessing;
		if (overAllQuantity < 0) {
			overAllQuantity = 0;
		}
		inBody.put("totalQuantity", overAllQuantity);
		exchange.setProperty("quantityDiff", overAllQuantity - sellableStockFromLazada);
		exchange.setProperty("isUpdateQuantityByDiff", isUpdateQuantityByDiff);
		exchange.setProperty("SKU", SKU);
		exchange.setProperty("individualListingSellerSKU", sellerSKU);
		exchange.setProperty("refrenceID", refrenceID);
		if (totalQuantity < 0) {
			totalQuantity = 0;
		}

		boolean isAutoStatusUpdate = false, isPartialAutoStatusUpdate = true;
		if(exchange.getProperty("isAutoStatusUpdate", Boolean.class)) {
			isAutoStatusUpdate = exchange.getProperty("isAutoStatusUpdate", Boolean.class);
		}
		if (exchange.getProperties().containsKey("isPartialAutoStatusUpdate")) {
			isPartialAutoStatusUpdate = exchange.getProperty("isPartialAutoStatusUpdate", Boolean.class);
		}
		boolean eligibleToUpdate = false;
		if (!status.equals(SIAInventoryStatus.NOT_LISTED.toString())) {
			if (totalQuantity <= 0 && !isPartialAutoStatusUpdate) {
				eligibleToUpdate = checkForAllChildStatus(SKU, accountNumber, nickNameID, exchange);
			} else if (isAutoStatusUpdate || !isPartialAutoStatusUpdate) {
				eligibleToUpdate = true;
			}
		}
		exchange.setProperty("eligibleToUpdateAutoStatus", eligibleToUpdate);

		if (eligibleToUpdate) {
			if (totalQuantity <= 0 && status.equals(SIAInventoryStatus.ACTIVE.toString())) {
				exchange.setProperty("statusToUpdate", "inactive");
				exchange.setProperty("updateToStatus", "inactive");
			}
			if (totalQuantity > 0 && status.equals(SIAInventoryStatus.INACTIVE.toString())) {
				exchange.setProperty("statusToUpdate", "active");
				exchange.setProperty("updateToStatus", "active");
			}
		}
		if (inBody.has("skuID")) {
			exchange.setProperty("skuID", inBody.getString("skuID"));
		}
		exchange.setProperty("quantity", quantity);
		exchange.setProperty("overAllQuantity", overAllQuantity);
		if (qtyUnderProcessing > 0) {
			exchange.setProperty("occupiedQuantity", qtyUnderProcessing);
		}
		exchange.setProperty("isUpdateStockViaProductUpdateApi", Config.getConfig().isUpdateStockViaProductUpdateApi());
		exchange.setProperty("isUpdateSellableStock", isUpdateSellableStock);
		exchange.setProperty("isQuantityUpdate", true);
		exchange.setProperty("impactSnapshot", inBody);

		if (inBody.has("quantities")) {
			exchange.setProperty("requestQuantities", inBody.get("quantities"));
		}

		log.info("Quantity update for quantityChange with accountNumber : "
				+ exchange.getProperty("accountNumber", String.class) + " , nickNameID : "
				+ exchange.getProperty("nickNameID", String.class) + " , SKU : " + SKU + " , sellerSKU : " + sellerSKU
				+ " and the quantity is : " + overAllQuantity);
	}

	private boolean checkForAllChildStatus(String SKU, String accountNumber, String nickNameID, Exchange exchange) {
		boolean eligibleToUpdate = true;
		DBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("SKU", Pattern.compile("^" + SKU.split("-")[0] + ".*"));

		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("status", new BasicDBObject("$ne", SIAInventoryStatus.REMOVED.toString()));
		elemMatch.put("nickNameID", nickNameID);
		elemMatch.put("variants", new BasicDBObject("$exists", false));
		searchQuery.put("lazada", new BasicDBObject("$elemMatch", elemMatch));

		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		List<DBObject> inventoryList = (List<DBObject>) table.find(searchQuery).toArray();
		List<String> customSKUList = new ArrayList<String>();
		for (int i = 0; i < inventoryList.size(); i++) {
			BasicDBObject inventory = (BasicDBObject) inventoryList.get(i);
			String invSKU = inventory.getString("SKU");
			if (invSKU.equals(SKU)) {
				continue;
			}
			List<BasicDBObject> lazada = (List<BasicDBObject>) inventory.get("lazada");
			BasicDBObject lazadaObj = (BasicDBObject) lazada.get(0);
			int noOfItem = lazadaObj.getInt("noOfItem");
			if (noOfItem > 0 && lazadaObj.getString("status").equals(SIAInventoryStatus.ACTIVE.toString())) {
				eligibleToUpdate = false;
				break;
			}
			customSKUList.add(inventory.getString("customSKU"));
		}
		exchange.setProperty("customSKUList", customSKUList);
		return eligibleToUpdate;
	}

}