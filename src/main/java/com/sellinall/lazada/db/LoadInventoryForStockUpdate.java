package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.enums.SIAInventoryStatus;

public class LoadInventoryForStockUpdate implements Processor {
	static Logger log = Logger.getLogger(LoadInventoryForStockUpdate.class.getName());

	public void process(Exchange exchange) throws Exception {
		Map<String, JSONObject> skuFeedMap = exchange.getProperty("skuFeedMap", Map.class);
		Map<String, List<DBObject>> countryBasedInventoryMap = new HashMap<String, List<DBObject>>();
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String countryCode = exchange.getProperty("countryCode", String.class);
		Boolean isGlobalAcc = false;
		if (countryCode.equals("GLOBAL")) {
			isGlobalAcc = true;
		}

		List<DBObject> inventoryList = loadInventoryDB(accountNumber, nickNameID, skuFeedMap.keySet());
		exchange.setProperty("isEligibleToProceed", true);
		if (inventoryList.size() == 0) {
			exchange.setProperty("isInventoryListEmpty", true);
			exchange.setProperty("isEligibleToProceed", false);
			return;
		}

		for (DBObject dbObject : inventoryList) {
			String SKU = dbObject.get("SKU").toString();
			String customSKU = dbObject.get("customSKU").toString();
			if (skuFeedMap.containsKey(SKU)) {
				JSONObject sukObj = skuFeedMap.get(SKU);
				sukObj.put("SKU", SKU);
				sukObj.put("customSKU", customSKU);

				Integer totalQuantity = 0;
				List<DBObject> channelObjArray = (List<DBObject>) dbObject.get("lazada");
				DBObject channelObj = channelObjArray.get(0);
				String status = channelObj.get("status").toString();
				if (channelObj.containsKey("itemID") && channelObj.containsKey("skuID")) {
					if (sukObj.has("totalQuantity")) {
						totalQuantity = sukObj.getInt("totalQuantity");
					}

					if (isGlobalAcc) {
						DBObject itemAmountObj = (DBObject) channelObj.get("itemAmount");
						String currencyCode = itemAmountObj.get("currencyCode").toString();
						countryCode = LazadaUtil.currencyToCountryCodeMap.get(currencyCode);
					}

					dbObject.put("itemID", channelObj.get("itemID").toString());
					dbObject.put("skuID", channelObj.get("skuID").toString());
					dbObject.put("overAllQuantity", totalQuantity);
					dbObject.put("status", status);
					dbObject.removeField("lazada");
					dbObject.put("sellerSKU", customSKU);

					if (countryBasedInventoryMap.containsKey(countryCode)) {
						List<DBObject> countryBaseOldInventoryList = countryBasedInventoryMap.get(countryCode);
						countryBaseOldInventoryList.add(dbObject);
						countryBasedInventoryMap.put(countryCode, countryBaseOldInventoryList);
					} else {
						List<DBObject> countryBaseNewInventoryList = new ArrayList<DBObject>();
						countryBaseNewInventoryList.add(dbObject);
						countryBasedInventoryMap.put(countryCode, countryBaseNewInventoryList);
					}
				}

				if (totalQuantity <= 0 && status.equals(SIAInventoryStatus.ACTIVE.toString())) {
					sukObj.put("updateToStatus", "inactive");
				}
				if (totalQuantity > 0 && status.equals(SIAInventoryStatus.INACTIVE.toString())) {
					sukObj.put("updateToStatus", "active");
				}
			}
		}

		exchange.setProperty("countryBasedInventoryMap", countryBasedInventoryMap);
		exchange.getOut().setBody(countryBasedInventoryMap.keySet());
	}

	private List<DBObject> loadInventoryDB(String accountNumber, String nickNameID, Set<String> skuList) {
		List<String> statusList = new ArrayList<String>(
				Arrays.asList(SIAInventoryStatus.ACTIVE.toString(), SIAInventoryStatus.INACTIVE.toString(),
						SIAInventoryStatus.SOLDOUT.toString(), SIAInventoryStatus.NOT_LISTED.toString()));

		DBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("SKU", new BasicDBObject("$in", skuList));
		DBObject elemMatch = new BasicDBObject();
		elemMatch.put("status", new BasicDBObject("$in", statusList));
		elemMatch.put("nickNameID", nickNameID);
		// only load non variant and variant child record
		elemMatch.put("variants", new BasicDBObject("$exists", false));
		searchQuery.put("lazada", new BasicDBObject("$elemMatch", elemMatch));

		DBCollection table = DbUtilities.getROInventoryDBCollection("inventory");
		return (List<DBObject>) table.find(searchQuery).toArray();

	}

}
