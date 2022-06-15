package com.sellinall.lazada.requests;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAInventoryUpdateStatus;

public class ProcessUpdateChildStatus implements Processor {

	public void process(Exchange exchange) throws Exception {
		boolean isPartialAutoStatusUpdate = true;
		if (exchange.getProperties().containsKey("isPartialAutoStatusUpdate")) {
			isPartialAutoStatusUpdate = exchange.getProperty("isPartialAutoStatusUpdate", Boolean.class);
		}
		Map<String, JSONObject> sellerSKUFeedMap = exchange.getProperty("sellerSKUFeedMap", LinkedHashMap.class);
		List<String> customSKUs = exchange.getProperty("invCustomSKUS", List.class);
		List<BasicDBObject> updateList = new ArrayList<BasicDBObject>();
		JSONArray customSKUListToPushProductMaster = new JSONArray();
		JSONArray customSKUListToPullProductMaster = new JSONArray();

		for (String customSKU : customSKUs) {
			if (sellerSKUFeedMap.containsKey(customSKU)) {
				JSONObject SKUObj = sellerSKUFeedMap.get(customSKU);
				String status = SKUObj.getString("status");
				String SKU = SKUObj.getString("SKU");
				String updateStatus = SKUObj.getString("updateToStatus");
				if (!isPartialAutoStatusUpdate && updateStatus.equalsIgnoreCase("inactive")) {
					continue;
				}

				BasicDBObject updateData = new BasicDBObject();
				updateData.put("SKU", SKU);
				updateData.put("lazada.$.timeLastUpdated", System.currentTimeMillis() / 1000L);

				if (status.equals("success")) {
					updateData.put("lazada.$.updateStatus", SIAInventoryUpdateStatus.COMPLETED.toString());
					updateData.put("lazada.$.failureReason", "");
					if (updateStatus.equals("active")) {
						updateData.put("lazada.$.status", SIAInventoryStatus.ACTIVE.toString());
						customSKUListToPushProductMaster.put(SKUObj.getString("customSKU"));
					} else {
						updateData.put("lazada.$.status", SIAInventoryStatus.INACTIVE.toString());
						customSKUListToPullProductMaster.put(SKUObj.getString("customSKU"));
					}
				} else {
					updateData.put("lazada.$.updateStatus", SIAInventoryUpdateStatus.FAILED.toString());
					updateData.put("lazada.$.failureReason", SKUObj.getString("failureReason"));
				}

				updateList.add(updateData);
			}
		}
		exchange.getOut().setBody(updateList);
		boolean isEligibleToUpdateProductMaster = false;
		if (customSKUListToPushProductMaster.length() > 0 || customSKUListToPullProductMaster.length() > 0) {
			isEligibleToUpdateProductMaster = true;
			exchange.setProperty("customSKUListToPushProductMaster", customSKUListToPushProductMaster);
			exchange.setProperty("customSKUListToPullProductMaster", customSKUListToPullProductMaster);
		}
		exchange.setProperty("isEligibleToUpdateProductMaster", isEligibleToUpdateProductMaster);
	}
}
