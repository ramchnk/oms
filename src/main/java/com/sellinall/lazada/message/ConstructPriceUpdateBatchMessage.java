package com.sellinall.lazada.message;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ConstructPriceUpdateBatchMessage implements Processor {
	static Logger log = Logger.getLogger(ConstructPriceUpdateBatchMessage.class.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		Map<String, JSONObject> sellerSKUPriceDetailsMap = exchange.getProperty("sellerSKUPriceDetailsMap",
				HashMap.class);
		HashMap<String, JSONArray> sellerUpdateMessageMap = exchange.getProperty("sellerUpdateMessageMap",
				HashMap.class);
		JSONArray data = new JSONArray();
		for (String sellerSKU : sellerSKUPriceDetailsMap.keySet()) {
			JSONObject singleRowUpdateMessage = new JSONObject();
			boolean isPostedSuccessfully = false;
			String updateMessage = "";
			JSONObject sellerSKUPriceDetails = sellerSKUPriceDetailsMap.get(sellerSKU);
			if (sellerUpdateMessageMap.containsKey(sellerSKU)) {
				JSONArray sellerUpdateMessageArray = sellerUpdateMessageMap.get(sellerSKU);
				for (int i = 0; i < sellerUpdateMessageArray.length(); i++) {
					JSONObject sellerUpdateMessage = sellerUpdateMessageArray.getJSONObject(i);
					if (!isPostedSuccessfully) {
						isPostedSuccessfully = sellerUpdateMessage.getBoolean("isPostedSuccefully");
					}
					updateMessage = updateMessage + sellerUpdateMessage.getString("SKU") + " - "
							+ sellerUpdateMessage.getString("updateMessage");
				}
			} else {
				updateMessage = "Inventory not found";
			}
			singleRowUpdateMessage.put("rowId", sellerSKUPriceDetails.get("rowId"));
			singleRowUpdateMessage.put("sellerSKU", sellerSKUPriceDetails.get("sellerSKU"));
			if (isPostedSuccessfully) {
				singleRowUpdateMessage.put("status", "success");
				singleRowUpdateMessage.put("warningMessage", updateMessage);
			} else {
				singleRowUpdateMessage.put("status", "failure");
				singleRowUpdateMessage.put("failureReason", updateMessage);
			}
			data.put(singleRowUpdateMessage);
		}
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		JSONObject message = new JSONObject();
		message.put("accountNumber", accountNumber);
		message.put("addendum", exchange.getProperty("addendum", JSONObject.class));
		message.put("data", data);
		if (exchange.getProperties().containsKey("requestType")) {
			message.put("requestType", exchange.getProperty("requestType", String.class));
		}
		log.info("price update batch message " + message.toString());
		exchange.getOut().setBody(message);
	}
}
