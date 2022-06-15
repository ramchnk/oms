package com.sellinall.lazada.requests;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.util.enums.PromotionType;

public class UpdatePromotionItemsInDB implements Processor {

	static Logger log = Logger.getLogger(UpdatePromotionItemsInDB.class.getName());

	public void process(Exchange exchange) throws Exception {
		String status = "failure";
		String action = exchange.getProperty("action", String.class);
		String failureReason = "";
		if (exchange.getProperties().containsKey("failureReason")) {
			failureReason = exchange.getProperty("failureReason", String.class);
		}
		if (exchange.getProperties().containsKey("isAddPromotionItemFlow")
				&& exchange.getProperty("isAddPromotionItemFlow", Boolean.class)
				&& exchange.getProperties().containsKey("addFailureReason")
				&& !exchange.getProperty("addFailureReason", String.class).isEmpty()) {
			if (!failureReason.isEmpty()) {
				failureReason += ", ";
			}
			failureReason += exchange.getProperty("addFailureReason", String.class);
		}
		if (exchange.getProperties().containsKey("isRemovePromotionItemFlow")
				&& exchange.getProperty("isRemovePromotionItemFlow", Boolean.class)
				&& exchange.getProperties().containsKey("removeFailureReason")
				&& !exchange.getProperty("removeFailureReason", String.class).isEmpty()) {
			if (!failureReason.isEmpty()) {
				failureReason += ", ";
			}
			failureReason += exchange.getProperty("removeFailureReason", String.class);
		}

		boolean isPromotionCreated = false;
		boolean isProductsAdded = false;
		if (exchange.getProperties().containsKey("isPromotionCreated")) {
			isPromotionCreated = exchange.getProperty("isPromotionCreated", Boolean.class);
		}
		if (exchange.getProperties().containsKey("isProductsAdded")) {
			isProductsAdded = exchange.getProperty("isProductsAdded", Boolean.class);
		}
		String promotionType = exchange.getProperty("promotionType", String.class);
		if (promotionType.equals(PromotionType.VOUCHER.toString())) {
			String voucherApplyFor = "";
			if (exchange.getProperties().containsKey("voucherApplyFor")) {
				voucherApplyFor = exchange.getProperty("voucherApplyFor", String.class);
			}
			if (voucherApplyFor.equals("SPECIFIC_PRODUCTS") && isPromotionCreated) {
				if (!isProductsAdded && !action.equals("updatePromotion")) {
					failureReason = "Failed to add skus" + (!failureReason.isEmpty() ? ", " + failureReason : "");
				}
				status = "success";
			} else if (voucherApplyFor.equals("ENTIRE_SHOP") && isPromotionCreated) {
				status = "success";
			} else if (exchange.getProperties().containsKey("isPromotionUpdated")
					&& exchange.getProperty("isPromotionUpdated", Boolean.class)) {
				status = "success";
			}
		} else if (promotionType.equals(PromotionType.FREE_SHIPPING.toString())) {
			String discountApplyFor = "";
			if (exchange.getProperties().containsKey("discountApplyFor")) {
				discountApplyFor = exchange.getProperty("discountApplyFor", String.class);
			}
			if (discountApplyFor.equals("SPECIFIC_PRODUCTS") && isPromotionCreated) {
				if (!isProductsAdded && !action.equals("updatePromotion")) {
					failureReason = "Failed to add skus" + (!failureReason.isEmpty() ? ", " + failureReason : "");
				}
				status = "success";
			} else if (discountApplyFor.equals("ENTIRE_SHOP") && isPromotionCreated) {
				status = "success";
			} else if (discountApplyFor.equals("CAMPAIGN_PRODUCTS") && isPromotionCreated) {
				status = "success";
			} else if (exchange.getProperties().containsKey("isPromotionUpdated")
					&& exchange.getProperty("isPromotionUpdated", Boolean.class)) {
				status = "success";
			}
		}
		String promotionID = null;
		if (exchange.getProperty("promotionID") != null) {
			promotionID = exchange.getProperty("promotionID", String.class);
		} else if (exchange.getProperties().containsKey("requestPromotionID")) {
			promotionID = exchange.getProperty("requestPromotionID", String.class);
		}

		String apiURL = Config.getConfig().getSIAPromotionServerURL() + "/promotions/status";
		exchange.setProperty("apiURL", apiURL);
		exchange.setProperty("CamelHttpMethod", "PUT");
		JSONObject payload = constructPromotionUpdate(promotionID, status, failureReason, exchange);
		exchange.getOut().setBody(payload);
	}

	private JSONObject constructPromotionUpdate(String promotionID, String status, String failureReason,
			Exchange exchange) throws JSONException {
		JSONObject payload = new JSONObject();
		payload.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		payload.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		payload.put("promotionID", promotionID);
		if (exchange.getProperties().containsKey("requestID")) {
			payload.put("objectId", exchange.getProperty("requestID", String.class));
		}
		payload.put("status", status);
		payload.put("requestType", exchange.getProperty("action"));
		String promotionType = exchange.getProperty("promotionType", String.class);
		payload.put("promotionType", promotionType);
		if (promotionType.equals(PromotionType.VOUCHER.toString())) {
			payload.put("voucherType", exchange.getProperty("voucherType", String.class));
		}
		if (exchange.getProperties().containsKey("itemRequestID")) {
			payload.put("requestID", exchange.getProperty("itemRequestID"));
		}
		if (!failureReason.isEmpty()) {
			payload.put("failureReason", failureReason);
		}
		boolean isAddItem = false, isRemoveItem = false;
		if (exchange.getProperties().containsKey("isAddPromotionItemFlow")
				&& exchange.getProperty("isAddPromotionItemFlow", Boolean.class)) {
			if (exchange.getProperties().containsKey("addFailedSKUs")) {
				payload.put("addFailedSKUs", exchange.getProperty("addFailedSKUs", ArrayList.class));
			} else if (!exchange.getProperties().containsKey("promotionID")) {
				payload.put("addFailedSKUs",
						new JSONArray(exchange.getProperty("addedPromotionItems", List.class).toString()));
			}
			isAddItem = true;
			if (exchange.getProperties().containsKey("requestedItemCount")) {
				payload.put("requestedItemCount", exchange.getProperty("requestedItemCount"));
			}
			if (exchange.getProperties().containsKey("isBulkVoucherEdit")) {
				payload.put("isBulkVoucherEdit", exchange.getProperty("isBulkVoucherEdit"));
			}
			if(exchange.getProperties().containsKey("isBulkFreeShippingEdit")) {
				payload.put("isBulkFreeShippingEdit", exchange.getProperty("isBulkFreeShippingEdit", Boolean.class));
			}
			if (exchange.getProperties().containsKey("documentObjectID")) {
				payload.put("documentObjectID", exchange.getProperty("documentObjectID"));
			}
		}
		if (exchange.getProperties().containsKey("isRemovePromotionItemFlow")
				&& exchange.getProperty("isRemovePromotionItemFlow", Boolean.class)) {
			if (exchange.getProperties().containsKey("isRemoveSKUsSuccess")
					&& exchange.getProperty("isRemoveSKUsSuccess", Boolean.class)) {
				payload.put("removeSKUs", exchange.getProperty("removedPromotionItems", ArrayList.class));
			}
			isRemoveItem = true;
		}
		if (isAddItem && isRemoveItem) {
			payload.put("updateType", "updateItem");
		} else if (isAddItem) {
			payload.put("updateType", "addItem");
		} else if (isRemoveItem) {
			payload.put("updateType", "removeItem");
		}
		return payload;
	}

}
