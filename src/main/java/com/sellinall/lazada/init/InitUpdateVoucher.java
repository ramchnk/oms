package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;

public class InitUpdateVoucher implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		String nickNameID = inBody.getString("nickNameID");
		String accountNumber = inBody.getString("accountNumber");
		JSONObject updateData = inBody.getJSONObject("data");
		String requestID = updateData.getString("requestID");
		String promotionID = updateData.getString("promotionID");
		String voucherType = updateData.getString("voucherType");
		JSONArray addedPromotionItems = updateData.getJSONArray("addedItems");
		JSONArray removedPromotionItems = updateData.getJSONArray("removedItems");

		if (updateData.has("requestedItemCount")) {
			exchange.setProperty("requestedItemCount", updateData.getInt("requestedItemCount"));
		}
		if (updateData.has("documentObjectID")) {
			exchange.setProperty("documentObjectID", updateData.getString("documentObjectID"));
		}
		if (updateData.has("isBulkVoucherEdit")) {
			exchange.setProperty("isBulkVoucherEdit", updateData.getBoolean("isBulkVoucherEdit"));
		}
		boolean isAddPromotionItem = false;
		if (addedPromotionItems.length() > 0) {
			isAddPromotionItem = true;
			exchange.setProperty("addedPromotionItems", LazadaUtil.JSONArrayToStringList(addedPromotionItems));
			exchange.setProperty("needToLoadInventory", true);
		}
		boolean isRemovePromotionItem = false;
		if (removedPromotionItems.length() > 0) {
			isRemovePromotionItem = true;
			exchange.setProperty("removedPromotionItems", LazadaUtil.JSONArrayToStringList(removedPromotionItems));
			exchange.setProperty("needToLoadInventory", true);
		}

		exchange.setProperty("accountNumber", accountNumber);
		exchange.setProperty("nickNameID", nickNameID);
		exchange.setProperty("promotionID", promotionID);
		exchange.setProperty("requestID", requestID);
		exchange.setProperty("voucherType", voucherType);
		exchange.setProperty("isAddPromotionItem", isAddPromotionItem);
		exchange.setProperty("isRemovePromotionItem", isRemovePromotionItem);
	}

}
