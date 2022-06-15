package com.sellinall.lazada.requests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;

public class UpdatePromotionDataToDB implements Processor {
	static Logger log = Logger.getLogger(UpdatePromotionDataToDB.class.getName());

	public void process(Exchange exchange) throws Exception {
		String requestID = exchange.getProperty("requestID", String.class);
		String status = "failure";
		String failureReason = "";
		if (exchange.getProperty("failureReason") != null
				&& !exchange.getProperty("failureReason", String.class).isEmpty()) {
			failureReason = exchange.getProperty("failureReason", String.class);
		} else {
			if (exchange.getProperty("isAddFlexiComboItemFlow") != null
					&& exchange.getProperty("isAddFlexiComboItemFlow", Boolean.class)) {
				if (exchange.getProperty("addFailureReason") != null
						&& !exchange.getProperty("addFailureReason", String.class).isEmpty()) {
					failureReason = exchange.getProperty("addFailureReason", String.class);

				}
			} else if (exchange.getProperty("isAddFlexiComboItemFlow") != null
					&& !exchange.getProperty("isAddFlexiComboItemFlow", Boolean.class)) {
				if (exchange.getProperty("removeFailureReason") != null
						&& !exchange.getProperty("removeFailureReason", String.class).isEmpty()) {
					failureReason = exchange.getProperty("removeFailureReason", String.class);
				}
			}
		}
		String discountApplyFor = "";
		if (exchange.getProperties().containsKey("discountApplyFor")) {
			discountApplyFor = exchange.getProperty("discountApplyFor", String.class);
		}
		boolean isPromotionCreated = false;
		boolean isProductsAdded = false;
		if (exchange.getProperty("isPromotionCreated") != null)
			isPromotionCreated = exchange.getProperty("isPromotionCreated", Boolean.class);
		if (exchange.getProperty("isProductsAdded") != null)
			isProductsAdded = exchange.getProperty("isProductsAdded", Boolean.class);
		if (discountApplyFor.equalsIgnoreCase("specificproducts") && isPromotionCreated && isProductsAdded) {
			status = "success";
		} else if (discountApplyFor.equalsIgnoreCase("entireshop") && isPromotionCreated) {
			status = "success";
		} else if (exchange.getProperties().containsKey("isPromotionUpdated")
				&& exchange.getProperty("isPromotionUpdated", Boolean.class)) {
			status = "success";
		}
		String promotionID = null;
		if (exchange.getProperty("promotionID") != null) {
			promotionID = exchange.getProperty("promotionID", String.class);
		}
		String url = Config.getConfig().getSIAPromotionServerURL() + "/promotions/status";
		Map<String, String> header = new HashMap<String, String>();
		header.put("Content-Type", "application/json");
		header.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		header.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		submitPromotionUpdate(url, header, promotionID, status, requestID, failureReason, exchange);
	}

	private void submitPromotionUpdate(String url, Map<String, String> header, String promotionID, String status,
			String requestID, String failureReason, Exchange exchange) throws JSONException {
		JSONObject payload = new JSONObject();
		payload.put("accountNumber", exchange.getProperty("accountNumber",String.class));
		payload.put("nickNameID", exchange.getProperty("nickNameID",String.class));
		payload.put("objectId", requestID);
		payload.put("promotionID", promotionID);
		payload.put("status", status);
		payload.put("requestType", exchange.getProperty("action"));
		payload.put("promotionType", "flexiCombo");
		if (!failureReason.isEmpty()) {
			payload.put("failureReason", failureReason);
		}
		if (exchange.getProperties().containsKey("itemRequestID")) {
			payload.put("requestID", exchange.getProperty("itemRequestID"));
		}
		if (exchange.getProperties().containsKey("isAddFlexiComboItemFlow")
				&& exchange.getProperty("isAddFlexiComboItemFlow", Boolean.class)) {
			if (exchange.getProperties().containsKey("addFailedSKUs")) {
				payload.put("addFailedSKUs", exchange.getProperty("addFailedSKUs", ArrayList.class));
			}
			payload.put("updateType", "addItem");
			if(exchange.getProperties().containsKey("isBulkFlexiComboEdit")) {
				payload.put("isBulkFlexiComboEdit", exchange.getProperty("isBulkFlexiComboEdit"));
			}
			if(exchange.getProperties().containsKey("documentObjectID")) {
				payload.put("documentObjectID", exchange.getProperty("documentObjectID"));
			}
			if(exchange.getProperties().containsKey("requestedItemCount")) {
				payload.put("requestedItemCount", exchange.getProperty("requestedItemCount"));
			}
		} else if (exchange.getProperties().containsKey("isAddFlexiComboItemFlow")
				&& !exchange.getProperty("isAddFlexiComboItemFlow", Boolean.class)) {
			if (exchange.getProperties().containsKey("isRemoveSKUsSuccess")
					&& exchange.getProperty("isRemoveSKUsSuccess", Boolean.class)) {
				payload.put("removeSKUs", exchange.getProperty("removedFlexiComboItems", ArrayList.class));
			}
			payload.put("updateType", "removeItem");
		}
		JSONObject response = new JSONObject();
		try {
			response = HttpsURLConnectionUtil.doPut(url, payload.toString(), header);
			JSONObject responsePayload = new JSONObject(response.getString("payload"));
			if (response.getInt("httpCode") == HttpStatus.SC_OK && responsePayload.has("response")
					&& responsePayload.getString("response").equals("success")) {
				log.info("promotionData updated for accountNumber : "
						+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
						+ exchange.getProperty("nickNameID", String.class));
			} else {
				log.error("promotionData update failed  for accountNumber : "
						+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
						+ exchange.getProperty("nickNameID", String.class) + " and response is" + response.toString());
			}
		} catch (Exception e) {
			log.error("promotionData update failed  for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response is" + response.toString(), e);
		}
	}
}
