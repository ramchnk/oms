package com.sellinall.lazada.message;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;

public class PrepareBatchMessage implements Processor {
	static Logger log = Logger.getLogger(PrepareBatchMessage.class.getName());


	public void process(Exchange exchange) throws Exception {
		JSONObject SKUMap = exchange.getProperty("SKUMap", JSONObject.class);
		String failureReason = "";
		String requestType = "";
		String warningMessage = "";
		ArrayList<JSONObject> SKUMaps = new ArrayList<JSONObject>();
		if (exchange.getProperties().containsKey("failureReason")
				&& !exchange.getProperty("failureReason", String.class).isEmpty()) {
			failureReason = exchange.getProperty("failureReason", String.class);
		} else if (exchange.getProperties().containsKey("updateFailureReason")
				&& !exchange.getProperty("updateFailureReason", String.class).isEmpty()) {
			failureReason = exchange.getProperty("updateFailureReason", String.class);
		}
		if (exchange.getProperties().containsKey("warningMessage")) {
			warningMessage = exchange.getProperty("warningMessage", String.class);
		}
		if (exchange.getProperties().containsKey("requestType")) {
			requestType = exchange.getProperty("requestType", String.class);
		}
		if (SKUMap.getString("SKU").contains("-")) {
			boolean postAsNonVariant = false;
			if (exchange.getProperties().containsKey("postAsNonVariant")) {
				postAsNonVariant = exchange.getProperty("postAsNonVariant", Boolean.class);
			}
			if (postAsNonVariant) {
				SKUMaps.add(constructBatchPostAsNonVariant(exchange, SKUMap, failureReason,warningMessage));
			} else {
				ArrayList<BasicDBObject> inventoryDetails = (ArrayList<BasicDBObject>) exchange
						.getProperty("inventoryDetails") != null ? (ArrayList<BasicDBObject>) exchange
								.getProperty("inventoryDetails") : new ArrayList<BasicDBObject>();
				// we need only number of child records so have to ignore parent
				// record
				JSONObject rowIdentifier = SKUMap.getJSONObject("rowIdentifier");
				String parentSKU = SKUMap.getString("SKU").split("-")[0];
				int rowId = rowIdentifier.getInt("rowId");
				if (exchange.getProperties().containsKey("editChildInventorySKUList")) {
					JSONArray editChildInventorySKUList = exchange.getProperty("editChildInventorySKUList",
							JSONArray.class);
					for (int i = 0; i < editChildInventorySKUList.length(); i++) {
						SKUMaps.add(constructMessage(rowIdentifier, rowId, editChildInventorySKUList.getString(i),
								failureReason, requestType, warningMessage));
						rowId++;
					}
				} else if (inventoryDetails.size() > 0) {
					for (int i = 1; i < inventoryDetails.size(); i++) {
						SKUMaps.add(constructMessage(rowIdentifier, rowId, parentSKU + "-" + String.format("%02d", i),
								failureReason, requestType, warningMessage));
						rowId++;
					}
				} else {
					if (!failureReason.isEmpty()) {
						SKUMap.put("status", "failure");
						SKUMap.put("failureReason", failureReason.replaceAll(requestType + " ", ""));
					} else {
						SKUMap.put("status", "success");
					}
					SKUMaps.add(SKUMap);
				}
			}
		} else {
			if (!failureReason.isEmpty()) {
				SKUMap.put("status", "failure");
				if (!warningMessage.isEmpty()) {
					failureReason = failureReason +"\n"+ warningMessage.replaceAll(requestType + " ", "");
				}
				SKUMap.put("failureReason", failureReason.replaceAll(requestType + " ", ""));
			} else {
				String status = "success";
				// for QC in lazada non-variant
				if (exchange.getProperty("requestType").equals("batchAddItem")) {
					status = "UNDER_REVIEW";
				}
				if (!warningMessage.isEmpty()) {
					SKUMap.put("warningMessage", warningMessage.replaceAll(requestType + " ", ""));
				}
				SKUMap.put("status", status);
			}
			SKUMaps.add(SKUMap);
		}
		log.debug("SKUMaps :" + SKUMaps);
		exchange.getOut().setBody(SKUMaps);
	}

	private static JSONObject constructBatchPostAsNonVariant(Exchange exchange, JSONObject SKUMap, String failureReason,String warningMessage)
			throws JSONException {
		ArrayList<BasicDBObject> list = exchange.getProperty("inventoryDetails", ArrayList.class);
		// List have only one one inventory
		BasicDBObject inventory = list.get(0);
		JSONObject rowIdentifier = SKUMap.getJSONObject("rowIdentifier");
		int rowId = rowIdentifier.getInt("rowId") - 1;
		String SKU = inventory.getString("SKU");
		int rowIndex = Integer.parseInt(SKU.split("-")[1]);
		return constructMessage(rowIdentifier, rowId + rowIndex, SKU, failureReason,
				exchange.getProperty("requestType", String.class), warningMessage);
	}

	private static JSONObject constructMessage(JSONObject rowIdentifier, int rowId, String SKU, String failureReason,
			String requestType,String warningMessage) throws JSONException {
		JSONObject batchMessage = new JSONObject();
		JSONObject formRowIdentifier = new JSONObject();
		formRowIdentifier.put("docId", rowIdentifier.getString("docId"));
		formRowIdentifier.put("documentObjectId", rowIdentifier.getString("documentObjectId"));
		formRowIdentifier.put("sheetId", rowIdentifier.getString("sheetId"));
		formRowIdentifier.put("rowId", rowId);
		batchMessage.put("SKU", SKU);
		batchMessage.put("rowIdentifier", formRowIdentifier);
		if (!failureReason.isEmpty()) {
			batchMessage.put("status", "failure");
			if (!warningMessage.isEmpty()) {
				failureReason = failureReason +"\n"+ warningMessage.replaceAll(requestType + " ", "");
			}
			batchMessage.put("failureReason", failureReason.replaceAll(requestType + " ", ""));
		} else {
			// for QC in lazada variant children and post as Non-variant
			String status = "success";
			if (requestType.equals("batchAddItem")) {
				status = "UNDER_REVIEW";
			}
			batchMessage.put("status", status);
			if(!warningMessage.isEmpty()){
				batchMessage.put("warningMessage", warningMessage.replaceAll(requestType + " ", ""));
			}
		}
		return batchMessage;
	}
}
