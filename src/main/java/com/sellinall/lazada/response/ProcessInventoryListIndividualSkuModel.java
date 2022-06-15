package com.sellinall.lazada.response;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.DateUtil;
import com.sellinall.util.enums.SIAInventoryStatus;

public class ProcessInventoryListIndividualSkuModel implements Processor {
	static Logger log = Logger.getLogger(ProcessInventoryListIndividualSkuModel.class.getName());

	public void process(Exchange exchange) throws Exception {
		ArrayList<BasicDBObject> inventoryList = new ArrayList<BasicDBObject>();
		BasicDBObject parentInventory = exchange.getIn().getBody(BasicDBObject.class);
		if (exchange.getProperty("itemHasVariants", Boolean.class) && parentInventory.containsField("lazada")
				&& !((ArrayList<BasicDBObject>) parentInventory.get("lazada")).isEmpty()) {
			BasicDBObject channelObj = (BasicDBObject) ((BasicDBList) parentInventory.get("lazada")).get(0);
			channelObj.remove("skuID");
			if (channelObj.containsKey("productImageURI")) {
			    ArrayList<String> parentImage = (ArrayList<String>) channelObj.get("productImageURI");
			    if (parentImage.size() == 0) {
			    	exchange.setProperty("isProductImageURINotExist", true);
			    }
			}
		}
		JSONArray customSKUListToPullProductMaster = exchange.getProperty("customSKUListToPullProductMaster",
				JSONArray.class);
		boolean isEligibleToUpdateProductMaster = exchange.getProperty("isEligibleToUpdateProductMaster",
				Boolean.class);
		inventoryList.add(parentInventory);
		JSONArray SKUs = new JSONArray();
		JSONObject itemFromSite = exchange.getProperty("item", JSONObject.class);
		if (itemFromSite.has("skus")) {
			SKUs = itemFromSite.getJSONArray("skus");
		} else {
			SKUs = itemFromSite.getJSONArray("Skus");
		}
		if (SKUs.length() == 1) {
			// item don't have variants
			JSONArray inventoryArray = new JSONArray();
			for(int j = 0; j < inventoryList.size(); j++) {
				inventoryArray.put(LazadaUtil.parseToJsonObject((DBObject) inventoryList.get(j)));
			}
			exchange.getOut().setBody(inventoryArray);
			return;
		}
		parentInventory.remove("customSKU");
		if (exchange.getProperties().containsKey("existingCustomSKU")) {
			parentInventory.put("customSKU", exchange.getProperty("existingCustomSKU", String.class));
		}
		String SKU = exchange.getProperty("SKU", String.class);
		String parentSKU = SKU.split("-")[0];
		if (SKU.contains("U-")) {
			parentSKU = parentSKU + "-" + SKU.split("-")[1];
		}
		SKU = parentSKU;
		String categoryID = "";
		if (itemFromSite.has("primary_category")) {
			categoryID = itemFromSite.getString("primary_category");
		} else if (itemFromSite.has("PrimaryCategory")) {
			categoryID = itemFromSite.getString("PrimaryCategory");
		}
		JSONObject attributes = new JSONObject();
		if (itemFromSite.has("attributes")) {
			attributes = itemFromSite.getJSONObject("attributes");
		} else {
			attributes = itemFromSite.getJSONObject("Attributes");
		}
		for (int i = 0; i < SKUs.length(); i++) {
			NumberFormat numberFormat = new DecimalFormat("00");
			String variantCounterString = numberFormat.format(i + 1).toString();
			inventoryList.add(processChild(exchange, attributes, SKUs.getJSONObject(i),
					SKU + "-" + variantCounterString, categoryID, customSKUListToPullProductMaster));
		}
		exchange.setProperty("customSKUListToPullProductMaster", customSKUListToPullProductMaster);
		/* update parent status, if anyone child has Active or Pending status */
		if (exchange.getProperties().containsKey("parentInventoryStatus")) {
			parentInventory = inventoryList.get(0);
			BasicDBObject channelObj = (BasicDBObject) ((BasicDBList) parentInventory.get("lazada")).get(0);
			channelObj.put("status", exchange.getProperty("parentInventoryStatus", String.class));
			channelObj.remove("shopSKU");
		}
		exchange.removeProperty("parentInventoryStatus");
		JSONArray inventoryArray = new JSONArray();
		for(int j = 0; j < inventoryList.size(); j++) {
			inventoryArray.put(LazadaUtil.parseToJsonObject((DBObject) inventoryList.get(j)));
		}
		exchange.getOut().setBody(inventoryArray);
	}

	private static BasicDBObject processChild(Exchange exchange, JSONObject attributes, JSONObject itemDetails,
			String SKU, String categoryID, JSONArray customSKUListToPullProductMaster) throws Exception {
		BasicDBObject inventory = new BasicDBObject();
		HashMap<String, Map<String, String>> siaImages = exchange.getProperty("siaImages", HashMap.class);
		String merchantID = exchange.getProperty("merchantID", String.class);
		inventory.put("SKU", SKU);
		String customSKU = itemDetails.getString("SellerSku");
		inventory.put("customSKU", customSKU);
		inventory.put("accountNumber", exchange.getProperty("accountNumber"));
		inventory.put("merchantId", merchantID);
		inventory.put("status", SIAInventoryStatus.ACTIVE.toString());
		inventory.put("date", System.currentTimeMillis() / 1000L);
		inventory.put("noOfItemsold", 0);
		inventory.put("noOfItemPending", 0);
		inventory.put("noOfItemRefunded", 0);
		inventory.put("noOfItemShipped", 0);
		inventory.put("sync", true);
		BasicDBObject channel = new BasicDBObject();
		inventory.put("itemTitle", attributes.getString("name"));
		channel.put("itemTitle", attributes.getString("name"));
		if (attributes.has("name_en")) {
			channel.put("itemTitleEnglish", attributes.getString("name_en"));
		}
		String status = itemDetails.getString("Status");
		if (status.equals("active")) {
			channel.put("status", SIAInventoryStatus.ACTIVE.toString());
		} else {
			// Non Active Item (Like a Out of stock)
			channel.put("status", SIAInventoryStatus.INACTIVE.toString());
			exchange.setProperty("isEligibleToUpdateProductMaster", true);
			customSKUListToPullProductMaster.put(customSKU);
		}
		String imageUrl = Config.getConfig().getUploadImageUri() + merchantID + "/";
		inventory.put("imageURL", imageUrl);
		channel.put("imageURL", imageUrl);
		String skuId = itemDetails.getString("SkuId");
		if (siaImages != null & !siaImages.get(skuId).isEmpty()) {
			BasicDBList imageURIList = getImageURIs(siaImages.get(skuId), SKU);
			inventory.put("imageURI", imageURIList);
			channel.put("imageURI", imageURIList);
		} else if (exchange.getProperties().containsKey("isProductImageURINotExist")
				&& exchange.getProperty("isProductImageURINotExist", boolean.class)) {
			// Image Missing
			channel.put("status", SIAInventoryStatus.IMAGEMISSING.toString());
		}
		if (attributes.has("description")) {
			inventory.put("itemDescription", attributes.getString("description"));
			channel.put("itemDescription", attributes.getString("description"));
		} else {
			inventory.put("itemDescription", "");
			channel.put("itemDescription", "");
		}
		if (attributes.has("description_en")) {
			channel.put("itemDescriptionEnglish", attributes.getString("description_en"));
		}
		if (attributes.has("short_description")) {
			inventory.put("shortDescription", attributes.getString("short_description"));
			channel.put("shortDescription", attributes.getString("short_description"));
		} else {
			inventory.put("shortDescription", "");
			channel.put("shortDescription", "");
		}
		if (attributes.has("short_description_en")) {
			channel.put("shortDescriptionEnglish", attributes.getString("short_description_en"));
		}
		String channelName = exchange.getProperty("channelName", String.class);
		inventory.put("site", channelName);
		if (!exchange.getProperty("isInventoryEmpty", Boolean.class)) {
			inventory.put("refrenceID", customSKU);
		}
		channel.put("categoryID", categoryID);
		channel.put("categoryName",
				LazadaUtil.getCategoryName(exchange.getProperty("countryCode", String.class), categoryID));
		if (attributes.has("brand")) {
			channel.put("brand", attributes.getString("brand"));
		}
		if (attributes.has("model")) {
			channel.put("model", attributes.getString("model"));
		}
		if (attributes.has("warranty_type")) {
			channel.put("warrantyType", attributes.getString("warranty_type"));
		}
		if (attributes.has("UPC")) {
			channel.put("UPC", attributes.getString("UPC"));
		}
		channel.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		if (itemDetails.has("Url")) {
			channel.put("itemUrl", itemDetails.getString("Url"));
		} else {
			// Item Waiting for Approval
			channel.put("status", SIAInventoryStatus.PENDING.toString());
		}
		if (!exchange.getProperties().containsKey("parentInventoryStatus")
				|| !exchange.getProperty("parentInventoryStatus").equals(SIAInventoryStatus.ACTIVE.toString())) {
			if (channel.getString("status").equals(SIAInventoryStatus.ACTIVE.toString())
					|| channel.getString("status").equals(SIAInventoryStatus.PENDING.toString())) {
				exchange.setProperty("parentInventoryStatus", channel.getString("status"));
			}
		}
		int noOfItem = 0;
		if (itemDetails.has("quantity")) {
			noOfItem = itemDetails.getInt("quantity");
		}
		inventory.put("noOfItem", noOfItem);
		channel.put("noOfItem", noOfItem);
		if (itemDetails.has("Available")) {
			channel.put("available", itemDetails.getInt("Available"));
		}
		if (itemDetails.has("SkuId")) {
			channel.put("skuID", itemDetails.getString("SkuId"));
		}
		if (exchange.getProperties().containsKey("parentItemID")
				&& exchange.getProperty("parentItemID", String.class) != null) {
			channel.put("itemID", exchange.getProperty("parentItemID", String.class));
		}
		if (itemDetails.has("price")) {
			double itemAmountStr = Double.parseDouble(itemDetails.getString("price"));
			long itemAmount = (long) Math.round((itemAmountStr * 100));
			channel.put("itemAmount",
					CurrencyUtil.getAmountObject(itemAmount, exchange.getProperty("currencyCode", String.class)));
		} else {
			channel.put("itemAmount",
					CurrencyUtil.getAmountObject(0, exchange.getProperty("currencyCode", String.class)));
		}
		String specialPriceDateFormat = "yyyy-MM-dd HH:mm";
		if (itemDetails.has("special_time_format")) {
			specialPriceDateFormat = itemDetails.getString("special_time_format");
		}
		String timeZoneOffset = LazadaUtil.timeZoneCountryMap.get(exchange.getProperty("countryCode", String.class));
		if (itemDetails.has("special_price") && Double.parseDouble(itemDetails.getString("special_price")) != 0.0) {
			double salePriceStr = Double.parseDouble(itemDetails.getString("special_price"));
			long saleAmount = (long) Math.round((salePriceStr * 100));
			channel.put("salePrice",
					CurrencyUtil.getAmountObject(saleAmount, exchange.getProperty("currencyCode", String.class)));
			if (itemDetails.has("special_from_time")) {
				channel.put("saleStartDate", DateUtil.getUnixTimestamp(itemDetails.getString("special_from_time"),
						specialPriceDateFormat, timeZoneOffset));
			}
			if (itemDetails.has("special_to_time")) {
				channel.put("saleEndDate", DateUtil.getUnixTimestamp(itemDetails.getString("special_to_time"),
						specialPriceDateFormat, timeZoneOffset));
			}
			channel.put("isPromotionEnabled", true);
		}
		// Process package details
		if (itemDetails.has("package_height")) {
			channel.put("packageHeight", itemDetails.getString("package_height"));
		}
		if (itemDetails.has("package_length")) {
			channel.put("packageLength", itemDetails.getString("package_length"));
		}
		if (itemDetails.has("package_width")) {
			channel.put("packageWidth", itemDetails.getString("package_width"));
		}
		if (itemDetails.has("package_weight")) {
			channel.put("packageWeight", itemDetails.getString("package_weight"));
		}
		if (itemDetails.has("package_content")) {
			channel.put("packageContent", itemDetails.getString("package_content"));
		}
		if (itemDetails.has("package_contents_en")) {
			channel.put("packageContentEnglish", itemDetails.getString("package_contents_en"));
		}

		channel.put("timeLastUpdated", System.currentTimeMillis() / 1000L);
		channel.put("refrenceID", customSKU);
		if (itemDetails.has("ShopSku")) {
			channel.put("shopSKU", itemDetails.getString("ShopSku"));
		}
		HashMap<String, String> attributeAndTypeMap = (HashMap<String, String>) exchange.getProperty("attributeAndTypeMap");
		BasicDBList itemSpecifics = processItemspecifics(attributes, itemDetails, attributeAndTypeMap);
		if (itemSpecifics.size() > 0) {
			channel.put("itemSpecifics", itemSpecifics);
		}
		if (exchange.getProperties().containsKey("linkToSKU")
				&& exchange.getProperties().containsKey("templateAttributes")) {
			BasicDBList templateAttributes = exchange.getProperty("templateAttributes", BasicDBList.class);
			channel.put("templateAttributes", templateAttributes);
		}
		if (exchange.getProperty("itemHasVariants", Boolean.class)) {
			Set<BasicDBObject> variantDetails = buildVariantsDetails(itemDetails,
					exchange.getProperty("validOrderVariantList", List.class));
			channel.put("variantDetails", variantDetails);
			inventory.put("variantDetails", variantDetails);
		}
		BasicDBList channelList = new BasicDBList();
		channelList.add(channel);
		inventory.put(channelName, channelList);
		return inventory;
	}

	private static BasicDBList getImageURIs(Map<String, String> imageMap, String SKU) throws JSONException {
		BasicDBList imageURIList = new BasicDBList();
		for (Map.Entry<String, String> entry : imageMap.entrySet()) {
			imageURIList.add("Shinmudra-" + SKU + "/" + entry.getValue());
		}
		return imageURIList;
	}

	private static BasicDBObject buildSiAItemSpecificFormat(String title, String name, HashMap<String, String> attributeAndTypeMap) {
		BasicDBObject itemSpecific = new BasicDBObject();
		if (attributeAndTypeMap.containsKey(title)) {
			itemSpecific.put("title", attributeAndTypeMap.get(title));
		} else {
			itemSpecific.put("title", title);
		}
		ArrayList<String> names = new ArrayList<String>();
		names.add(name);
		itemSpecific.put("names", names);
		return itemSpecific;
	}

	private static BasicDBList processItemspecifics(JSONObject attributes, JSONObject itemDetails, HashMap<String, String> attributeAndTypeMap)
			throws JSONException {
		BasicDBList itemSpecifics = new BasicDBList();
		String attributesMandatoryFields = "name-description-short_description-brand-model-warranty_type-name_en-description_en-short_description_en-source";
		String[] attributesMandatoryList = attributesMandatoryFields.split("-");
		List<String> attributesList = Arrays.asList(attributesMandatoryList);
		Iterator<?> keys = attributes.keys();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			if (!attributesList.contains(key)) {
				itemSpecifics.add(buildSiAItemSpecificFormat(key, attributes.getString(key), attributeAndTypeMap));
			}
		}
		// Will handle variants based item Specifics values
		String SKUMandatoryFields = "Status-quantity-_compatible_variation_-SellerSku-package_content"
				+ "Url-package_width-package_height-special_price-price-package_length-special_from_date-package_weight-Available"
				+ "special_to_date-Images-package_contents_en-ShopSku-fblWarehouseInventories-special_time_format-multiWarehouseInventories-channelInventories-SkuId-special_to_time-special_from_time";
		keys = itemDetails.keys();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			if (!SKUMandatoryFields.contains(key)) {
				itemSpecifics.add(buildSiAItemSpecificFormat(key, itemDetails.getString(key), attributeAndTypeMap));
			}
		}
		return itemSpecifics;
	}

	private static Set<BasicDBObject> buildVariantsDetails(JSONObject SKU, List<String> validOrderVariantList)
			throws JSONException {
		Set<BasicDBObject> variants = new HashSet<BasicDBObject>();
		for (int i = 0; i < validOrderVariantList.size(); i++) {
			String variantName = validOrderVariantList.get(i);
			if (SKU.has(variantName)) {
				BasicDBObject variant = new BasicDBObject();
				variant.put("title", variantName);
				variant.put("name", SKU.get(variantName));
				variants.add(variant);
			} else if (validOrderVariantList.size() == 1 && !SKU.getString("_compatible_variation_").equals("...")) {
				BasicDBObject variant = new BasicDBObject();
				variant.put("title", variantName);
				variant.put("name", SKU.get("_compatible_variation_"));
				variants.add(variant);
			}
		}
		return variants;
	}

}