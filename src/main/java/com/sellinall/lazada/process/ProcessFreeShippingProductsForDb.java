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

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.enums.PromotionType;
import com.sellinall.lazada.util.ProcessPromotionDetails;

public class ProcessFreeShippingProductsForDb implements Processor {

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		JSONObject vocuherItemsWithProduct = exchange.getProperty("freeShippingItemWithProduct", JSONObject.class);
		Map<String, JSONObject> itemDetailsSKUIDmap = new HashMap<String, JSONObject>();
		if (vocuherItemsWithProduct.has("products")) {
			itemDetailsSKUIDmap = ProcessPromotionDetails.getSKUIDDtails(
					LazadaUtil.JSONArrayToStringList(vocuherItemsWithProduct.getJSONArray("products")), accountNumber,
					nickNameID);
		}
		convetToDBStructure(vocuherItemsWithProduct, itemDetailsSKUIDmap, exchange);
	}

	public static void convetToDBStructure(JSONObject freeShippingItems, Map<String, JSONObject> itemDetailsSKUIDmap,
			Exchange exchange) throws JSONException {
		JSONObject freeShipping = new JSONObject();
		freeShipping.put("promotionType", PromotionType.FREE_SHIPPING.toString());
		if (freeShippingItems.has("period_end_time")) {
			freeShipping.put("endDate", freeShippingItems.getLong("period_end_time") / 1000);
		}
		if (freeShippingItems.has("period_start_time")) {
			freeShipping.put("startDate", freeShippingItems.getLong("period_start_time") / 1000);
		}
		freeShipping.put("promotionName", freeShippingItems.getString("promotion_name"));

		freeShipping.put("promotionID", freeShippingItems.getString("id"));
		if (freeShippingItems.has("template_type") && !(freeShippingItems.getString("template_type").equals("null"))) {
			freeShipping.put("templateType", freeShippingItems.getString("template_type"));
		}
		if (freeShippingItems.has("template_code") && !(freeShippingItems.getString("template_code").equals("null"))) {
			freeShipping.put("templateCode", freeShippingItems.getString("template_code"));
		}
		freeShipping.put("currencyCode", freeShippingItems.getString("currency"));

		if (freeShippingItems.has("category_name") && !(freeShippingItems.getString("category_name").equals("null"))) {
			freeShipping.put("categoryName", freeShippingItems.getString("category_name"));
		}
		freeShipping.put("discountApplyFor", freeShippingItems.getString("apply"));
		if (freeShippingItems.has("budget_value")) {
			freeShipping.put("budgetValue", freeShippingItems.getString("budget_value"));
		}
		if (freeShippingItems.has("campaign_tag") && !(freeShippingItems.getString("campaign_tag").equals("null"))) {
			freeShipping.put("campaignTag", freeShippingItems.getString("campaign_tag"));
		}
		
		freeShipping.put("regionType", freeShippingItems.getString("region_type"));
		if (freeShippingItems.has("region_value") && freeShippingItems.getJSONArray("region_value") != null) {
			freeShipping.put("regionValue", freeShippingItems.getJSONArray("region_value"));
		}
		if (freeShippingItems.has("promo_tier") && freeShippingItems.get("promo_tier") != null) {
			JSONObject promo_tier = freeShippingItems.getJSONObject("promo_tier");
			freeShipping.put("tiers", promo_tier.get("tiers"));
			freeShipping.put("dealCriteria", promo_tier.get("deal_criteria"));
			freeShipping.put("discountType", promo_tier.get("discount_type"));
		}
		if (freeShippingItems.has("used_budget_value")
				&& !(freeShippingItems.getString("used_budget_value").equals("null"))) {
			freeShipping.put("usedBudgetValue", freeShippingItems.getString("used_budget_value"));
		}
		if (freeShippingItems.has("platformChannel")
				&& !(freeShippingItems.getString("platformChannel").equals("null"))) {
			freeShipping.put("platformChannel", freeShippingItems.getString("platform_channel"));
		}
		if (freeShippingItems.has("products")) {
			freeShipping.put("itemDetails", itemDetailsForSKUID(
					LazadaUtil.JSONArrayToStringList(freeShippingItems.getJSONArray("products")), itemDetailsSKUIDmap));
		}
		freeShipping.put("budgetType", freeShippingItems.getString("budget_type"));
		freeShipping.put("periodType", freeShippingItems.getString("period_type"));
		freeShipping.put("deliveryOption", freeShippingItems.getString("delivery_option"));
		freeShipping.put("status", freeShippingItems.getString("status"));
		freeShipping.put("timeCreated", System.currentTimeMillis() / 1000);
		exchange.getOut().setBody(freeShipping);
	}

	public static ArrayList<String> JSONArrayToArrayList(JSONArray data) throws JSONException {
		ArrayList<String> listOfData = new ArrayList<String>();
		for (int i = 0; i < data.length(); i++) {
			listOfData.add(data.getString(i));
		}
		return listOfData;
	}

	public static List<JSONObject> itemDetailsForSKUID(List<String> SKUIDLists,
			Map<String, JSONObject> itemDetailsSKUIDmap) {
		List<JSONObject> itemDetailsList = new ArrayList<JSONObject>();
		for (String SKUID : SKUIDLists) {
			if (itemDetailsSKUIDmap.containsKey(SKUID)) {
				JSONObject itemDetail = itemDetailsSKUIDmap.get(SKUID);
				if (itemDetail != null) {
					itemDetailsList.add(itemDetail);
				}
			}
		}
		return itemDetailsList;
	}
}
