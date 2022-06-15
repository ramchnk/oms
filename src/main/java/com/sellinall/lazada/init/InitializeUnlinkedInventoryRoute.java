package com.sellinall.lazada.init;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;

public class InitializeUnlinkedInventoryRoute implements Processor {
	static Logger log = Logger.getLogger(InitializeUnlinkedInventoryRoute.class.getName());

	public void process(Exchange exchange) throws Exception {
		log.debug("Inside InitializeUnlinkedInventoryRoute");
		JSONObject item = exchange.getProperty("item", JSONObject.class);
		JSONArray SKUs = new JSONArray();
		if(item.has("skus")) {
			SKUs = item.getJSONArray("skus");
		} else {
			SKUs = item.getJSONArray("Skus");
		}
		exchange.setProperty("SKUS", SKUs);
		JSONObject itemSepcifics = SKUs.getJSONObject(0);
		String sellerSKU = itemSepcifics.getString("SellerSku");
		exchange.setProperty("sellerSKU", sellerSKU);
		exchange.setProperty("refrenceID", sellerSKU);
		exchange.setProperty("itemHasVariants", false);
		String categoryId = "";
		if (item.has("primary_category")) {
			categoryId = item.getString("primary_category");
		} else {
			categoryId = item.getString("PrimaryCategory");
		}
		JSONObject lookupResponse = getCategoryResponse(exchange, categoryId);
		if (SKUs.length() > 1) {
			exchange.setProperty("itemHasVariants", true);
			exchange.setProperty("SIAVariantsDetails", LazadaUtil.getVariantsFromCategory(lookupResponse));
		}
		JSONArray attributes = LazadaUtil.getCategoryAttributes(lookupResponse);
		exchange.setProperty("attributeAndTypeMap", LazadaUtil.constructAttributesAndAttributeTypeMap(attributes));
	}

	private static JSONObject getCategoryResponse(Exchange exchange, String categoryID) throws JSONException {
		Map<String, String> header = new HashMap<String, String>();
		header.put("Content-Type", "application/json");
		header.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		header.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		String totalURL = Config.getConfig().getSIAListingLookupServerURL() + "/lazada/category/"
				+ exchange.getProperty("countryCode", String.class) + "/" + categoryID;
		log.info(totalURL + ", for account : " + exchange.getProperty("accountNumber"));
		try {
			JSONObject lookupJSON = HttpsURLConnectionUtil.doGet(totalURL, header);
			lookupJSON = new JSONObject(lookupJSON.getString("payload"));
			return lookupJSON;
		} catch (Exception exception) {
			log.error("Failed to get category attributes for categoryID : " + categoryID + ", refrenceID : "
					+ exchange.getProperty("refrenceID", String.class) + ", for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class));
		}
		return new JSONObject();
	}
}