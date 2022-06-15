package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mudra.sellinall.config.APIUrlConfig;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.enums.SIAInventoryStatus;

public class InitPriceUpdateBySellerSKU implements Processor {

	@Override
	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		List<DBObject> inventoryList = (List<DBObject>) exchange.getProperty("inventoryList");
		List<BasicDBObject> payloadList = new ArrayList<BasicDBObject>();
		Map<String, JSONObject> sellerSKUPriceDetailsMap = exchange.getProperty("sellerSKUPriceDetailsMap",
				HashMap.class);
		for (DBObject inventoryObj : inventoryList) {
			BasicDBObject inventory = (BasicDBObject) inventoryObj;
			bulidPayloadData(inventory, sellerSKUPriceDetailsMap, payloadList, exchange);

		}
		exchange.setProperty("payloadList", payloadList);
		if (exchange.getProperties().containsKey("countryCode")) {
			String timeZoneOffset = LazadaUtil.timeZoneCountryMap.get(exchange.getProperty("countryCode", String.class));
			exchange.setProperty("timeZoneOffset", timeZoneOffset);
		}
	}

	public void bulidPayloadData(BasicDBObject inventory, Map<String, JSONObject> sellerSKUPriceDetailsMap,
			List<BasicDBObject> payloadList, Exchange exchange) throws JSONException {
		String customSKU = inventory.getString("customSKU");
		BasicDBObject sellerSKUObject = BasicDBObject.parse(sellerSKUPriceDetailsMap.get(customSKU).toString());
		BasicDBObject itemAmount = (BasicDBObject) sellerSKUObject.get("itemAmount");
		List<BasicDBObject> lazadaList = (List<BasicDBObject>) inventory.get("lazada");
		BasicDBObject lazada = lazadaList.get(0);
		if (exchange.getProperties().containsKey("isGlobalAccount")
				&& exchange.getProperty("isGlobalAccount", Boolean.class)) {
			String countryCode = LazadaUtil.currencyToCountryCodeMap.get(itemAmount.get("currencyCode").toString());
			exchange.setProperty("countryCode", countryCode);
			exchange.setProperty("currencyCode", itemAmount.get("currencyCode").toString());
			exchange.setProperty("hostURL", APIUrlConfig.getNewAPIUrl(countryCode));
		}
		if (lazada.getString("status").equals(SIAInventoryStatus.NOT_LISTED.toString())) {
			// as this is not listed product, so we won't add this in request payload
			return;
		}
		BasicDBObject payloadObj = new BasicDBObject();
		payloadObj.put("sellerSKU", customSKU);
		payloadObj.put("itemID", lazada.getString("itemID"));
		payloadObj.put("skuID", lazada.getString("skuID"));
		payloadObj.put("itemAmount", itemAmount);
		if (sellerSKUObject.containsField("salePrice")) {
			BasicDBObject salePrice = (BasicDBObject) sellerSKUObject.get("salePrice");
			payloadObj.put("salePrice", salePrice);
		}
		if (sellerSKUObject.containsField("saleStartDate")) {
			payloadObj.put("saleStartDate", sellerSKUObject.getLong("saleStartDate"));
		}
		if (sellerSKUObject.containsField("saleEndDate")) {
			payloadObj.put("saleEndDate", sellerSKUObject.getLong("saleEndDate"));
		}

		payloadList.add(payloadObj);
	}
}
