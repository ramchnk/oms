package com.sellinall.lazada.response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.DateUtil;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAUnlinkedInventoryStatus;

public class ProcessParentInventory implements Processor {
	static Logger log = Logger.getLogger(ProcessParentInventory.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject itemFromSite = exchange.getProperty("item", JSONObject.class);
		JSONObject attributes = new JSONObject();
		JSONArray SKUs = new JSONArray();
		if(itemFromSite.has("attributes")){
			attributes = itemFromSite.getJSONObject("attributes");
		} else {
			attributes = itemFromSite.getJSONObject("Attributes");
		}
		BasicDBObject inventory = new BasicDBObject();
		if(itemFromSite.has("skus")) {
			SKUs = itemFromSite.getJSONArray("skus");
		}else {
			SKUs = itemFromSite.getJSONArray("Skus");
		}
		JSONObject itemDetails = SKUs.getJSONObject(0);
		HashMap<String, Map<String, String>> siaImages = exchange.getProperty("siaImages", HashMap.class);
		String merchantID = exchange.getProperty("merchantID", String.class);
		String SKU = exchange.getProperty("SKU", String.class);
		String customSKU = itemDetails.getString("SellerSku");
		inventory.put("SKU", SKU);
		inventory.put("customSKU", customSKU);
		inventory.put("accountNumber", exchange.getProperty("accountNumber"));
		inventory.put("merchantId", merchantID);
		if (exchange.getProperty("isInventoryEmpty", Boolean.class)) {
			inventory.put("status", SIAInventoryStatus.ACTIVE.toString());
		} else {
			inventory.put("status", SIAUnlinkedInventoryStatus.UNLINKED.toString());
			if (exchange.getProperties().containsKey("autoLinkFailureReason")) {
				inventory.put("failureReason", exchange.getProperty("autoLinkFailureReason"));
			}
		}
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
			// Non Active Item (Like a Out of stack)
			channel.put("status", SIAInventoryStatus.INACTIVE.toString());
		}

		if (itemDetails.has("SkuId")) {
			channel.put("skuID", itemDetails.getString("SkuId"));
		}

		inventory.put("imageURL", Config.getConfig().getUploadImageUri() + merchantID + "/");
		String skuId = itemDetails.getString("SkuId");
		if (siaImages.containsKey(skuId) && !siaImages.get(skuId).isEmpty()) {
			BasicDBList imageURIList = getImageURIs(siaImages.get(skuId), SKU);
			inventory.put("imageURI", imageURIList);
			channel.put("imageURI", imageURIList);
		} else if (exchange.getProperty("itemHasVariants", Boolean.class)) {
			// If it is parent then will directly get image from first child
			// parent doesn't have any special image
			BasicDBList imageURIList = getImageURIs(siaImages.get(skuId), SKU + "-01");
			if (imageURIList.size() > 0) {
				inventory.put("imageURI", imageURIList);
				channel.put("imageURI", imageURIList);
			}
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
		if (!exchange.getProperty("isInventoryEmpty", Boolean.class)) {
			//Unlinked inventory only have this property
			inventory.put("site", channelName);
			inventory.put("refrenceID", customSKU);
		}
		String categoryId = "";
		if (itemFromSite.has("primary_category")) {
			categoryId = itemFromSite.getString("primary_category");
		} else {
			categoryId = itemFromSite.getString("PrimaryCategory");
		}
		String categoryName = LazadaUtil.getCategoryName(
				exchange.getProperty("countryCode", String.class), categoryId);
		channel.put("categoryID", categoryId);
		channel.put("categoryName", categoryName);
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
		int noOfItem = itemDetails.getInt("quantity");
		inventory.put("noOfItem", noOfItem);
		channel.put("noOfItem", noOfItem);
		if (itemDetails.has("Available")) {
			channel.put("available", itemDetails.getInt("Available"));
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
		if(itemDetails.has("package_height")){
			channel.put("packageHeight", itemDetails.getString("package_height"));
		}
		if(itemDetails.has("package_length")){
			channel.put("packageLength", itemDetails.getString("package_length"));
		}
		if(itemDetails.has("package_width")){
			channel.put("packageWidth", itemDetails.getString("package_width"));
		}
		if(itemDetails.has("package_weight")){
			channel.put("packageWeight", itemDetails.getString("package_weight"));
		}
		if(itemDetails.has("package_content")){
			channel.put("packageContent", itemDetails.getString("package_content"));
		}
		if (itemDetails.has("package_contents_en")) {
			channel.put("packageContentEnglish", itemDetails.getString("package_contents_en"));
		}
		channel.put("timeLastUpdated", System.currentTimeMillis() / 1000L);
		channel.put("refrenceID", customSKU);
		channel.put("itemID", exchange.getProperty("parentItemID",String.class));
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
			ArrayList<BasicDBObject> variants = buildVariants(exchange, SKUs,
					exchange.getProperty("SIAVariantsDetails", JSONArray.class));
			if (variants.size() > 0) {
				channel.put("variants", variants);
				inventory.put("variants", variants);
			}
		}
		BasicDBList channelList = new BasicDBList();
		channelList.add(channel);
		inventory.put(channelName, channelList);
		exchange.getOut().setBody(inventory);
	}

	private BasicDBList getImageURIs(Map<String, String> imageMap, String SKU) throws JSONException {
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
		List<String> attributesList =  Arrays.asList(attributesMandatoryList);
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

	private static ArrayList<BasicDBObject> buildVariants(Exchange exchange, JSONArray SKUs,
			JSONArray SIAVariantsDetails) throws JSONException {
		ArrayList<BasicDBObject> variants = new ArrayList<BasicDBObject>();
		List<String> validOrderVariantList = LazadaUtil.getValidVariantOrder(SKUs, SIAVariantsDetails);
		exchange.setProperty("validOrderVariantList", validOrderVariantList);
		for (int i = 0; i < validOrderVariantList.size(); i++) {
			String variantName = validOrderVariantList.get(i);
			Set<Object> names = new LinkedHashSet<Object>();
			for (int child = 0; child < SKUs.length(); child++) {
				JSONObject childDetails = SKUs.getJSONObject(child);
				if (childDetails.has(variantName)) {
					names.add(childDetails.get(variantName));
				} else if (SIAVariantsDetails.length() == 1
						&& !childDetails.getString("_compatible_variation_").equals("...")) {
					// some time item has only one level of variants then we
					// will get here
					// EX: Size only
					names.add(childDetails.get("_compatible_variation_"));
				}
			}
			if (names.size() > 0) {
				BasicDBObject variant = new BasicDBObject();
				variant.put("title", variantName);
				variant.put("names", names);
				variants.add(variant);
			}
		}
		return variants;
	}
}
