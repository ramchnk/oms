package com.sellinall.lazada.init;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;

public class InitializeVoucher implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("accountNumber", inBody.getString("accountNumber"));
		exchange.setProperty("nickNameID", inBody.getString("nickNameID"));
		if (inBody.has("requestID")) {
			exchange.setProperty("requestID", inBody.getString("requestID"));
		}
		if (inBody.has("data")) {
			JSONObject data = inBody.getJSONObject("data");
			if (exchange.getProperty("action", String.class).equals("createPromotion")) {
				exchange.setProperty("requestPromotionID", data.getString("promotionID"));
			} else if (data.has("promotionID")) {
				exchange.setProperty("promotionID", data.getString("promotionID"));
			}
			if (data.has("requestID")) {
				exchange.setProperty("itemRequestID", data.getString("requestID"));
			}
			if (data.has("requestedItemCount")) {
				exchange.setProperty("requestedItemCount", data.getInt("requestedItemCount"));
			}
			if (data.has("documentObjectID")) {
				exchange.setProperty("documentObjectID", data.getString("documentObjectID"));
			}
			if (data.has("isBulkVoucherEdit")) {
				exchange.setProperty("isBulkVoucherEdit", data.getBoolean("isBulkVoucherEdit"));
			}
			if (data.has("promotionName")) {
				exchange.setProperty("promotionName", data.getString("promotionName"));
			}
			exchange.setProperty("voucherType", data.getString("voucherType"));
			if (data.has("voucherCollectDate")) {
				exchange.setProperty("voucherCollectDate", data.getLong("voucherCollectDate"));
			}
			if (data.has("startDate")) {
				exchange.setProperty("startDate", data.getLong("startDate"));
			}
			if (data.has("endDate")) {
				exchange.setProperty("endDate", data.getLong("endDate"));
			}
			if (data.has("timeCreated")) {
				exchange.setProperty("timeCreated", data.getLong("timeCreated"));
			}
			boolean isEligibleToUpdateVoucher = false;
			if (data.has("criteriaOverMoney")) {
				isEligibleToUpdateVoucher = true;
				exchange.setProperty("criteriaOverMoney", data.getString("criteriaOverMoney"));
			}
			exchange.setProperty("isEligibleToUpdateVoucher", isEligibleToUpdateVoucher);
			if (data.has("voucherApplyFor")) {
				exchange.setProperty("voucherApplyFor", data.getString("voucherApplyFor"));
			}
			if (data.has("voucherDisplayArea")) {
				exchange.setProperty("voucherDisplayArea", data.getString("voucherDisplayArea"));
			}
			if (data.has("voucherDiscountType")) {
				exchange.setProperty("voucherDiscountType", data.getString("voucherDiscountType"));
			}
			if (data.has("voucherLimitPerCustomer")) {
				exchange.setProperty("voucherLimitPerCustomer", data.getInt("voucherLimitPerCustomer"));
			}
			if(data.has("offeringMoneyValueOff")) {
				exchange.setProperty("offeringMoneyValueOff", data.getString("offeringMoneyValueOff"));
			}
			if (data.has("issued")) {
				exchange.setProperty("issued", data.getInt("issued"));
			}
			if (data.has("maxDiscountOfferingMoneyValue")) {
				exchange.setProperty("maxDiscountOfferingMoneyValue", data.getString("maxDiscountOfferingMoneyValue"));
			}
			if (data.has("offeringPercentageDiscountOff")) {
				exchange.setProperty("offeringPercentageDiscountOff", data.getInt("offeringPercentageDiscountOff"));
			}
			if (data.has("addedItems")) {
				boolean isAddPromotionItem = false;
				JSONArray addedPromotionItems = data.getJSONArray("addedItems");
				if (addedPromotionItems.length() > 0) {
					isAddPromotionItem = true;
					List<String> skusList = LazadaUtil.JSONArrayToStringList(addedPromotionItems);
					exchange.setProperty("addedPromotionItems", skusList);
					exchange.setProperty("skus", skusList);
					exchange.setProperty("needToLoadInventory", true);
				}
				exchange.setProperty("isAddPromotionItem", isAddPromotionItem);
			}
			if (data.has("removedItems")) {
				boolean isRemovePromotionItem = false;
				JSONArray removedPromotionItems = data.getJSONArray("removedItems");
				if (removedPromotionItems.length() > 0) {
					isRemovePromotionItem = true;
					exchange.setProperty("removedPromotionItems", LazadaUtil.JSONArrayToStringList(removedPromotionItems));
					exchange.setProperty("needToLoadInventory", true);
				}
				exchange.setProperty("isRemovePromotionItem", isRemovePromotionItem);
			}
		}
	}

}
