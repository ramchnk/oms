package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.util.JSON;

public class InitChannelBatchRoute implements Processor {
	static Logger log = Logger.getLogger(InitChannelBatchRoute.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		log.debug("InitLazadaBatchAddItemRoute in body: " + inBody);
		exchange.setProperty("accountNumber", inBody.get("accountNumber"));
		if (inBody.has("siteNicknames")) {
			JSONArray siteNicknames = inBody.getJSONArray("siteNicknames");
			exchange.setProperty("siteNicknames", siteNicknames);
			exchange.setProperty("channelName", siteNicknames.getString(0).split("-")[0]);
		} else if (inBody.has("nickNameID")) {
			exchange.setProperty("nickNameID", inBody.getString("nickNameID"));
			exchange.setProperty("channelName", inBody.getString("nickNameID").split("-")[0]);
		}
		exchange.setProperty("requestType", inBody.getString("requestType"));
		exchange.setProperty("inputRequest", inBody);
		if (inBody.has("isValidate")) {
			exchange.setProperty("isValidate", inBody.getBoolean("isValidate"));
		}
		if (inBody.has("sheetType")) {
			exchange.setProperty("sheetType", inBody.getString("sheetType"));
		}
		boolean isPriceUpdate = false, isStatusUpdate = false, isBulkImagesUpdate = false;
		if (inBody.has("fieldsToUpdate")) {
			List<String> fieldsToUpdate = (ArrayList<String>) JSON
					.parse(inBody.getJSONArray("fieldsToUpdate").toString());
			exchange.setProperty("fieldsToUpdate", fieldsToUpdate);
			if (fieldsToUpdate.contains("price")) {
				isPriceUpdate = true;
			}
			if (fieldsToUpdate.contains("images") && fieldsToUpdate.size() == 1) {
				isBulkImagesUpdate = true;
			}
			if (fieldsToUpdate.contains("status")) {
				exchange.setProperty("updateToStatus", inBody.getString("updateToStatus"));
				exchange.setProperty("failureReasons", new ArrayList<String>());
				isStatusUpdate = true;
			}
		}
		if (inBody.has("editChildInventorySKUList")) {
			exchange.setProperty("editChildInventorySKUList", inBody.getJSONArray("editChildInventorySKUList"));
		}
		exchange.setProperty("isPriceUpdate", isPriceUpdate);
		exchange.setProperty("isStatusUpdate", isStatusUpdate);
		exchange.setProperty("isBulkImagesUpdate", isBulkImagesUpdate);
		exchange.getOut().setBody(inBody);
	}
}
