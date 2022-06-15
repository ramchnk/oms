package com.sellinall.lazada.db;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAInventoryUpdateStatus;

public class UpdateDeactivateStatusInDB implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		String SKU = exchange.getProperty("SKU", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		boolean isPostingSuccess = exchange.getProperty("isPostingSuccess", Boolean.class);
		String failureReason = "";
		if (exchange.getProperties().containsKey("failureReason")) {
			failureReason = exchange.getProperty("failureReason", String.class);
		}
		updateStatusInDB(accountNumber, SKU, nickNameID, isPostingSuccess, failureReason);

		JSONArray productCustomSKUList = exchange.getProperty("productCustomSKUList", JSONArray.class);
		exchange.setProperty("customSKUListToPushProductMaster", new JSONArray());
		exchange.setProperty("customSKUListToPullProductMaster", productCustomSKUList);
		exchange.setProperty("isEligibleToUpdateProductMaster", true);

		Map<String, JSONObject> sellerSKUFeedMap = exchange.getProperty("sellerSKUFeedMap", LinkedHashMap.class);
		for (int i = 0; i < productCustomSKUList.length(); i++) {
			if (sellerSKUFeedMap.containsKey(productCustomSKUList.getString(i))) {
				JSONObject feedMessage = sellerSKUFeedMap.get(productCustomSKUList.getString(i));
				if (failureReason.isEmpty()) {
					feedMessage.put("status", "success");
				} else {
					feedMessage.put("status", "failure");
					feedMessage.put("failureReason", failureReason);
				}
			}
		}
	}

	private void updateStatusInDB(String accountNumber, String SKU, String nickNameID, boolean isPostingSuccess,
			String failureReason) {
		DBObject query = new BasicDBObject();
		query.put("accountNumber", accountNumber);
		query.put("SKU", new BasicDBObject("$regex", SKU));
		query.put("lazada.nickNameID", nickNameID);

		DBObject setObj = new BasicDBObject();
		if (isPostingSuccess) {
			setObj.put("lazada.$.status", SIAInventoryStatus.INACTIVE.toString());
		} else {
			setObj.put("lazada.$.updateStatus", SIAInventoryUpdateStatus.FAILED.toString());
			setObj.put("lazada.$.failureReason", failureReason);
		}

		DBObject updateObj = new BasicDBObject();
		updateObj.put("$set", setObj);

		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		table.updateMulti(query, updateObj);
	}

}
