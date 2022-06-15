package com.sellinall.lazada.init;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class InitProcessInventoryItem implements Processor {
	static Logger log = Logger.getLogger(InitProcessInventoryItem.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accountNumber = "";
		exchange.setProperty("hasItemExists", false);
		if (exchange.getProperties().containsKey("accountNumber")) {
			accountNumber = (String) exchange.getProperty("accountNumber");
		}
		String nickNameID = "";
		if (exchange.getProperties().containsKey("nickNameID")) {
			nickNameID = (String) exchange.getProperty("nickNameID");
		}
		JSONObject itemFromSite = new JSONObject();
		Object inBody = exchange.getIn().getBody();
		if (inBody instanceof JSONObject) {
			itemFromSite = (JSONObject) exchange.getIn().getBody();
		} else {
			ArrayList<JSONObject> arrayList = (ArrayList<JSONObject>) exchange.getProperty("pulledInventoryList");
			itemFromSite = arrayList.get(exchange.getProperty("itemListIndex", Integer.class));
		}
		exchange.setProperty("item", itemFromSite);

		JSONArray skus = new JSONArray();
		if (itemFromSite.has("skus")) {
			skus = (JSONArray) itemFromSite.get("skus");
		} else if (itemFromSite.has("Skus")) {
			skus = (JSONArray) itemFromSite.get("Skus");
		}
		if (skus.length() < 1) {
			log.error("No more item found for ItemId : " + exchange.getProperty("parentItemID", String.class)
					+ ",  account : " + accountNumber + ", nickNameID: " + nickNameID + " and itemFromSite : "
					+ itemFromSite.toString());
			return;
		}
		exchange.setProperty("hasItemExists", true);
		String itemStatus = skus.getJSONObject(0).getString("Status");
		exchange.setProperty("itemStatus", itemStatus);
		exchange.removeProperty("templateAttributes");
		String customSKU = skus.getJSONObject(0).getString("SellerSku");
		exchange.setProperty("SellerSku", customSKU);
		log.info("currently processing customSKU : " + customSKU + ", with status : " + itemStatus + ", for account : "
				+ accountNumber + ", for nickNameID: " + nickNameID);
	}

}
