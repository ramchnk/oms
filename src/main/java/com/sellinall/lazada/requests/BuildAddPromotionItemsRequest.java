package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.enums.PromotionType;

public class BuildAddPromotionItemsRequest implements Processor {

	static Logger log = Logger.getLogger(BuildAddPromotionItemsRequest.class.getName());
	static int maxRetryCount = 3;

	public void process(Exchange exchange) throws Exception {
		String accessToken = exchange.getProperty("accessToken", String.class);
		JSONArray skusList = exchange.getProperty("skusList", JSONArray.class);
		String promotionID = exchange.getProperty("promotionID", String.class);
		String promotionType = exchange.getProperty("promotionType", String.class);
		Map<String, String> SKUIDMap = exchange.getProperty("SKUIDMap", HashMap.class);

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("id", promotionID);
		map.put("sku_ids", skusList.toString());

		String queryParams = "&id=" + promotionID;
		queryParams += "&sku_ids=" + URLEncoder.encode(skusList.toString(), "UTF-8");
		if (promotionType.equals(PromotionType.VOUCHER.toString())) {
			String voucherType = exchange.getProperty("voucherType", String.class);
			map.put("voucher_type", voucherType);
			queryParams += "&voucher_type=" + voucherType;
			exchange.setProperty("apiName", "/promotion/voucher/product/sku/add");
		} else if (promotionType.equals(PromotionType.FREE_SHIPPING.toString())) {
			exchange.setProperty("apiName", "/promotion/freeshipping/product/sku/add");
		}

		callAPI(exchange, map, queryParams, accessToken, promotionID, 1);

		if (!exchange.getProperties().containsKey("addFailureReason")) {
			if (exchange.getProperties().containsKey("responseData")) {
				JSONObject responseData = exchange.getProperty("responseData", JSONObject.class);
				List<String> addFailedSKUs = getFailedSKUList(SKUIDMap, responseData);
				if (addFailedSKUs.size() > 0) {
					exchange.setProperty("addFailedSKUs", addFailedSKUs);
				}
			}
		} else {
			exchange.setProperty("addFailedSKUs", new ArrayList(SKUIDMap.values()));
		}
	}

	private void callAPI(Exchange exchange, HashMap<String, String> map, String queryParams, String accessToken,
			String promotionID, int retryCount) {
		String hostURL = exchange.getProperty("hostURL", String.class);
		String apiName = exchange.getProperty("apiName", String.class);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = "";
		try {
			response = NewLazadaConnectionUtil.callAPI(hostURL, apiName, accessToken, map, "", queryParams, "POST",
					clientID, clientSecret);
			JSONObject serviceResponse = new JSONObject(response);
			if (serviceResponse.has("code") && serviceResponse.getString("code").equals("0")) {
				exchange.setProperty("isProductsAdded", true);
				exchange.setProperty("isPromotionUpdated", true);
				if (serviceResponse.has("data")) {
					if (serviceResponse.getJSONObject("data").length() > 0) {
						// from lazada only failure SKU's will return when
						// adding flexiCombo Item
						log.error("Updated products to promotion for promotionID " + promotionID
								+ " and accountNumber : " + exchange.getProperty("accountNumber", String.class)
								+ " and nickNameID : " + exchange.getProperty("nickNameID", String.class)
								+ " and response is" + response.toString());
					}
					exchange.setProperty("responseData", serviceResponse.getJSONObject("data"));
				} else if (retryCount <= maxRetryCount) {
					retryCount++;
					callAPI(exchange, map, queryParams, accessToken, promotionID, retryCount);
				}
				return;
			}
			exchange.setProperty("isPromotionUpdated", false);
			log.error("Failed to add products to promotion for promotionID " + promotionID + " and accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response is" + response.toString());
			if (serviceResponse.has("message")) {
				exchange.setProperty("addFailureReason", serviceResponse.getString("message"));
			}
			exchange.setProperty("isProductsAdded", false);
		} catch (Exception e) {
			exchange.setProperty("isProductsAdded", false);
			log.error("Exception ocurred while adding products to promotion for promotionID " + promotionID
					+ " accountNumber : " + exchange.getProperty("accountNumber", String.class) + " and nickNameID is "
					+ exchange.getProperty("nickNameID", String.class) + " and response is " + response.toString());
			e.printStackTrace();
		}
	}

	private static List<String> getFailedSKUList(Map<String, String> SKUIDMap, JSONObject responseData) {
		List<String> failedSKUs = new ArrayList<String>();
		Iterator<String> iterator = responseData.keys();
		// responseData contains only failure SKU's when adding to voucher
		while (iterator.hasNext()) {
			String key = (String) iterator.next();
			if (SKUIDMap != null && SKUIDMap.containsKey(key)) {
				failedSKUs.add(SKUIDMap.get(key));
			}
		}
		return failedSKUs;
	}

}
