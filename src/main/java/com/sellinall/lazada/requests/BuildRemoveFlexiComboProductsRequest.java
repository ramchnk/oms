package com.sellinall.lazada.requests;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class BuildRemoveFlexiComboProductsRequest implements Processor {
	static Logger log = Logger.getLogger(BuildRemoveFlexiComboProductsRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		callRemoveFlexiComboProductsAPI(exchange);
	}

	private void callRemoveFlexiComboProductsAPI(Exchange exchange) throws IOException, JSONException {
		exchange.setProperty("isAddFlexiComboItemFlow", false);
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
			response = NewLazadaConnectionUtil.callAPI(hostURL, "/promotion/flexicombo/products/delete", accessToken,
					map, "", queryParams, "POST", clientID, clientSecret);
			JSONObject serviceResponse = new JSONObject(response);
			if (serviceResponse.has("code") && serviceResponse.getString("code").equals("0")) {
				exchange.setProperty("isRemoveSKUsSuccess", true);
				exchange.setProperty("isPromotionUpdated", true);
				return;
			}
			exchange.setProperty("isPromotionUpdated", false);
			log.error("Failed to remove products from flexi combo  for promotionID " + promotionID + " and accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response is" + response.toString());
			if (serviceResponse.has("message")) {
				exchange.setProperty("removeFailureReason", serviceResponse.getString("message"));
			}
		} catch (Exception e) {
			log.error("Exception ocurred while remove products remove flexi combo  for promotionID " + promotionID
					+ " accountNumber : " + exchange.getProperty("accountNumber", String.class) + " and nickNameID is "
					+ exchange.getProperty("nickNameID", String.class) + " and response is " + response.toString(), e);
		}
	}

}
