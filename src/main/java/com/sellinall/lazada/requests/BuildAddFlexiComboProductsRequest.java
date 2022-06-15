package com.sellinall.lazada.requests;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class BuildAddFlexiComboProductsRequest implements Processor {
	static Logger log = Logger.getLogger(BuildAddFlexiComboProductsRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		callAddFlexiComboProductsAPI(exchange);
	}

	private void callAddFlexiComboProductsAPI(Exchange exchange) throws IOException, JSONException {
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		JSONArray skusList = exchange.getProperty("skusList", JSONArray.class);
		String promotionID = exchange.getProperty("promotionID", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("id", promotionID);
		map.put("sku_ids", skusList.toString());
		map.put("access_token", accessToken);
		String queryParams = "";
		queryParams += "&id=" + promotionID;
		queryParams += "&sku_ids=" + URLEncoder.encode(skusList.toString(), "UTF-8");
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = "";
		try {
			response = NewLazadaConnectionUtil.callAPI(hostURL, "/promotion/flexicombo/products/add", accessToken, map,
					"", queryParams, "POST", clientID, clientSecret);
			JSONObject serviceResponse = new JSONObject(response);
			if (serviceResponse.has("code") && serviceResponse.getString("code").equals("0")) {
				exchange.setProperty("isProductsAdded", true);
				exchange.setProperty("isPromotionUpdated", true);
				if (serviceResponse.has("data")) {
					//from lazada only failure SKU's will return when adding flexiCombo Item
					log.error("Updated products to flexi combo  for promotionID " + promotionID
							+ " and accountNumber : " + exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
							+ exchange.getProperty("nickNameID", String.class) + " and response is" + response.toString());
					exchange.setProperty("responseData", serviceResponse.getJSONObject("data"));
				}
				return;
			}
			exchange.setProperty("isPromotionUpdated", false);
			log.error("Failed to add products to flexi combo  for promotionID " + promotionID
					+ " and accountNumber : " + exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response is" + response.toString());
			if (serviceResponse.has("message")) {
				exchange.setProperty("addFailureReason", serviceResponse.getString("message"));
			}
			exchange.setProperty("isProductsAdded", false);
		} catch (Exception e) {
			log.error("Exception ocurred while adding products to flexi combo  for promotionID " + promotionID
					+ " accountNumber : " + exchange.getProperty("accountNumber", String.class) + " and nickNameID is "
					+ exchange.getProperty("nickNameID", String.class) + " and response is "+ response.toString(), e);
			exchange.setProperty("isProductsAdded", false);
		}
	}

}