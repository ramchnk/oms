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

public class ProcessVoucherProductsForDb implements Processor {

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		JSONObject vocuherItemsWithProduct = exchange.getProperty("voucherItemWithProduct", JSONObject.class);
		Map<String, JSONObject> itemDetailsSKUIDmap = new HashMap<String, JSONObject>();
		if (vocuherItemsWithProduct.has("products")) {
			itemDetailsSKUIDmap = ProcessPromotionDetails.getSKUIDDtails(
					LazadaUtil.JSONArrayToStringList(vocuherItemsWithProduct.getJSONArray("products")), accountNumber,
					nickNameID);
		}
		convetToDBStructure(vocuherItemsWithProduct, itemDetailsSKUIDmap, exchange);
	}
	public static void convetToDBStructure(JSONObject voucherItems, Map<String, JSONObject> itemDetailsSKUIDmap,
			Exchange exchange) throws JSONException {
		JSONObject voucher = new JSONObject();
		voucher.put("promotionType", PromotionType.VOUCHER.toString());
		voucher.put("endDate", voucherItems.getLong("period_end_time") / 1000);
		if (voucherItems.has("max_discount_offering_money_value")
				&& !voucherItems.getString("max_discount_offering_money_value").equals("null")) {
			voucher.put("maxDiscountOfferingMoneyValue", voucherItems.getString("max_discount_offering_money_value"));
		}
		voucher.put("voucherApplyFor", voucherItems.getString("apply"));
		voucher.put("promotionName", voucherItems.getString("voucher_name"));
		if (voucherItems.has("voucher_code")) {
			voucher.put("voucherCode", voucherItems.getString("voucher_code"));
		}
		if (voucherItems.has("offering_money_value_off")
				&& !voucherItems.getString("offering_money_value_off").equals("null")) {
			voucher.put("offeringMoneyValueOff", voucherItems.getString("offering_money_value_off"));
		}
		if (voucherItems.has("offering_percentage_discount_off")
				&& !voucherItems.getString("offering_percentage_discount_off").equals("null")) {
			voucher.put("offeringPercentageDiscountOff", voucherItems.getString("offering_percentage_discount_off"));
		}
		if (voucherItems.has("collect_start")) {
			voucher.put("voucherCollectDate", Long.parseLong(voucherItems.getString("collect_start")) / 1000L);
		}
		voucher.put("promotionID", voucherItems.getString("id"));
		voucher.put("voucherLimitPerCustomer", voucherItems.getString("limit"));
		voucher.put("issued", voucherItems.getString("issued"));
		voucher.put("voucherDiscountType", voucherItems.getString("voucher_discount_type"));
		voucher.put("voucherType", voucherItems.getString("voucher_type"));
		voucher.put("voucherDisplayArea", voucherItems.getString("display_area"));
		voucher.put("startDate", voucherItems.getLong("period_start_time") / 1000);
		voucher.put("timeCreated", System.currentTimeMillis() / 1000);
		voucher.put("criteriaOverMoney", voucherItems.getString("criteria_over_money"));
		voucher.put("status", voucherItems.getString("status"));
		if (voucherItems.has("products")) {
			voucher.put("itemDetails", itemDetailsForSKUID(
					LazadaUtil.JSONArrayToStringList(voucherItems.getJSONArray("products")), itemDetailsSKUIDmap));
		}
		exchange.getOut().setBody(voucher);
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
