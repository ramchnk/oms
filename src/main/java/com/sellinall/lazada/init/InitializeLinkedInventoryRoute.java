package com.sellinall.lazada.init;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;

public class InitializeLinkedInventoryRoute implements Processor {
	static Logger log = Logger.getLogger(InitializeLinkedInventoryRoute.class.getName());

	public void process(Exchange exchange) throws Exception {
		log.debug("Inside InitializeUnlinkedInventoryRoute");
		JSONObject item = (JSONObject) exchange.getIn().getBody();
		String SKU = "";
		exchange.setProperty("item", item);
		JSONArray SKUs = item.getJSONArray("Skus");
		exchange.setProperty("SKUS", SKUs);
		JSONObject inventoryFromSite = SKUs.getJSONObject(0);
		if (exchange.getProperties().containsKey("linkToSKU")) {
			SKU = exchange.getProperty("linkToSKU", String.class);
		} else {
			SKU = inventoryFromSite.getString("SellerSku");
		}
		exchange.setProperty("sellerSKU", SKU);
		if (SKUs.length() > 1) {
			exchange.setProperty("itemHasVariants", true);
			exchange.setProperty("SIAVariantsDetails",
					getCategoryAttributes(exchange, item.getString("PrimaryCategory")));
		} else {
			exchange.setProperty("itemHasVariants", false);
		}
		exchange.setProperty("isNewItem", false);

	}
	
	private static JSONArray getCategoryAttributes(Exchange exchange, String categoryID)
			throws JSONException, IOException {
		Map<String, String> header = new HashMap<String, String>();
		header.put("Content-Type", "application/json");
		header.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		header.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		String totalURL = Config.getConfig().getSIAListingLookupServerURL() + "/lazada/category/"
				+ exchange.getProperty("countryCode", String.class) + "/" + categoryID;
		log.info(totalURL);
		JSONObject lookupJSON = HttpsURLConnectionUtil.doGet(totalURL, header);
		lookupJSON = new JSONObject(lookupJSON.getString("payload"));
		if (lookupJSON.has("variants")) {
			return lookupJSON.getJSONArray("variants");
		}
		return new JSONArray();
	}
}