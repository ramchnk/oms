package com.sellinall.lazada.init;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;

public class InitializeFreeShipping implements Processor {
	static Logger log = Logger.getLogger(InitializeFreeShipping.class);

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("accountNumber", inBody.getString("accountNumber"));
		exchange.setProperty("nickNameID", inBody.getString("nickNameID"));
		exchange.setProperty("requestID", inBody.getString("requestID"));
		if (inBody.has("data")) {
			JSONObject data = inBody.getJSONObject("data");
			if (data.has("promotionID") && exchange.getProperty("action", String.class).equals("createPromotion")) {
				exchange.setProperty("requestPromotionID", data.getString("promotionID"));
			} else if (data.has("promotionID")) {
				exchange.setProperty("promotionID", data.getString("promotionID"));
			}
			if(data.has("isBulkFreeShippingEdit")) {
				exchange.setProperty("isBulkFreeShippingEdit", data.getBoolean("isBulkFreeShippingEdit"));
			}
			if(data.has("requestID")) {
				exchange.setProperty("itemRequestID", data.getString("requestID"));
			}
			if (data.has("requestedItemCount")) {
				exchange.setProperty("requestedItemCount", data.getInt("requestedItemCount"));
			}
			if(data.has("documentObjectID")) {
				exchange.setProperty("documentObjectID", data.getString("documentObjectID"));
			}
			if (data.has("budgetType")) {
				exchange.setProperty("budgetType", data.getString("budgetType"));
			}
			String templateType = null;
			if (data.has("templateType")) {
				templateType = data.getString("templateType");
				exchange.setProperty("templateType", data.getString("templateType"));
			}
			String discountApplyFor = null;
			if (data.has("discountApplyFor")) {
				discountApplyFor = data.getString("discountApplyFor");
				exchange.setProperty("discountApplyFor", data.getString("discountApplyFor"));
			}
			if (data.has("templateCode")) {
				exchange.setProperty("templateCode", data.getString("templateCode"));
			}
			if (data.has("categoryName")) {
				exchange.setProperty("categoryName", data.getString("categoryName"));
			}
			if (data.has("budgetValue")) {
				exchange.setProperty("budgetValue", data.getString("budgetValue"));
			}
			if (data.has("promotionName")) {
				exchange.setProperty("promotionName", data.getString("promotionName"));
			}
			String periodType = null;
			if (data.has("periodType")) {
				periodType = data.getString("periodType");
				exchange.setProperty("periodType", periodType);
			}
			if (data.has("regionType")) {
				exchange.setProperty("regionType", data.getString("regionType"));
			}
			if (periodType != null && periodType.equals("SPECIAL_PERIOD")) {
				if (data.has("startDate")) {
					exchange.setProperty("startDate", data.getLong("startDate"));
				}
				if (data.has("endDate")) {
					exchange.setProperty("endDate", data.getLong("endDate"));
				}
			}
			if (data.has("platformChannel")) {
				exchange.setProperty("platformChannel", data.getString("platformChannel"));
			}
			if (data.has("campaignTag") && ((templateType != null && templateType.equals("CAMPAIGN"))
					|| (discountApplyFor != null && discountApplyFor.equals("CAMPAIGN_PRODUCTS")))) {
				exchange.setProperty("campaignTag", data.getString("campaignTag"));
			}
			if (data.has("regionValue")) {
				exchange.setProperty("regionValue", data.getJSONArray("regionValue"));
			}
			if (data.has("deliveryOption")) {
				exchange.setProperty("deliveryOption", data.getString("deliveryOption"));
			}
			if (data.has("tiers")) {
				exchange.setProperty("tiers", data.getJSONArray("tiers"));
			}
			if (data.has("discountType")) {
				exchange.setProperty("discountType", data.getString("discountType"));
			}
			boolean isEligibleToUpdateFreeShipping = false;
			if (data.has("dealCriteria")) {
				isEligibleToUpdateFreeShipping = true;
				exchange.setProperty("dealCriteria", data.getString("dealCriteria"));
			}
			exchange.setProperty("isEligibleToUpdateFreeShipping", isEligibleToUpdateFreeShipping);
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
