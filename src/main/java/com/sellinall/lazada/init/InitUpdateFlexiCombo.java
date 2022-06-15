package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;

public class InitUpdateFlexiCombo implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		String nickNameID = inBody.getString("nickNameID");
		String accountNumber = inBody.getString("accountNumber");
		JSONObject updateData = inBody.getJSONObject("data");
		String promotionID = updateData.getString("promotionID");
		JSONArray addedFlexiComboItems = updateData.getJSONArray("addedItems");
		JSONArray removedFlexiComboItems = updateData.getJSONArray("removedItems");
		if (updateData.has("requestID")) {
			exchange.setProperty("itemRequestID", updateData.getString("requestID"));
		}
		if (updateData.has("requestedItemCount")) {
			exchange.setProperty("requestedItemCount", updateData.getInt("requestedItemCount"));
		}
		if (updateData.has("documentObjectID")) {
			exchange.setProperty("documentObjectID", updateData.getString("documentObjectID"));
		}
		if (updateData.has("isBulkFlexiComboEdit")) {
			exchange.setProperty("isBulkFlexiComboEdit", updateData.getBoolean("isBulkFlexiComboEdit"));
		}
		exchange.setProperty("nickNameID", nickNameID);
		exchange.setProperty("accountNumber", accountNumber);
		exchange.setProperty("promotionID", promotionID);
		boolean isAddFlexiComboItem = false;
		if (addedFlexiComboItems.length() > 0) {
			isAddFlexiComboItem = true;
			exchange.setProperty("addedFlexiComboItems", LazadaUtil.JSONArrayToStringList(addedFlexiComboItems));
			exchange.setProperty("needToLoadInventory", true);
		}
		boolean isRemoveFlexiComboItem = false;
		if (removedFlexiComboItems.length() > 0) {
			isRemoveFlexiComboItem = true;
			exchange.setProperty("removedFlexiComboItems", LazadaUtil.JSONArrayToStringList(removedFlexiComboItems));
			exchange.setProperty("needToLoadInventory", true);
		}
		exchange.setProperty("isAddFlexiComboItem", isAddFlexiComboItem);
		exchange.setProperty("isRemoveFlexiComboItem", isRemoveFlexiComboItem);
	}

}
