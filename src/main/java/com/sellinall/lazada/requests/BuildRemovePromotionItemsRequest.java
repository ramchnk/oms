package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.enums.PromotionType;

public class BuildRemovePromotionItemsRequest implements Processor {

	static Logger log = Logger.getLogger(BuildRemovePromotionItemsRequest.class.getName());
	static int maxRetryCount = 3;

	public void process(Exchange exchange) throws Exception {
		String accessToken = exchange.getProperty("accessToken", String.class);
		JSONArray skusList = exchange.getProperty("skusList", JSONArray.class);
		String promotionID = exchange.getProperty("promotionID", String.class);
		String promotionType = exchange.getProperty("promotionType", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("id", promotionID);
		map.put("sku_ids", skusList.toString());
		
		String queryParams = "";
		queryParams += "&id=" + promotionID;
		queryParams += "&sku_ids=" + URLEncoder.encode(skusList.toString(), "UTF-8");
		if (promotionType.equals(PromotionType.VOUCHER.toString())) {
			String voucherType = exchange.getProperty("voucherType", String.class);
			map.put("voucher_type", voucherType);
			queryParams += "&voucher_type=" + voucherType;
			exchange.setProperty("apiName", "/promotion/voucher/product/sku/remove");
		} else if (promotionType.equals(PromotionType.FREE_SHIPPING.toString())) {
			exchange.setProperty("apiName", "/promotion/freeshipping/product/sku/remove");
		}
		callAPI(exchange, map, queryParams, accessToken, promotionID, 1);
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
				exchange.setProperty("isRemoveSKUsSuccess", true);
				exchange.setProperty("isPromotionUpdated", true);
				if (serviceResponse.has("success") && serviceResponse.getString("success").equals("true")) {
					log.info("Removed products from voucher for promotionID " + promotionID + " and accountNumber : "
							+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
							+ exchange.getProperty("nickNameID", String.class) + " and response is"
							+ response.toString());
				} else {
					if (retryCount <= maxRetryCount) {
						retryCount++;
						callAPI(exchange, map, queryParams, accessToken, promotionID, retryCount);
					}
				}
				return;
			}
			exchange.setProperty("isPromotionUpdated", false);
			log.error("Failed to remove products from promotion for promotionID " + promotionID + " and accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response is" + response.toString());
			if (serviceResponse.has("message")) {
				exchange.setProperty("removeFailureReason", serviceResponse.getString("message"));
			}
		} catch (Exception e) {
			exchange.setProperty("isPromotionUpdated", false);
			log.error("Exception ocurred while remove products from promotion for promotionID " + promotionID
					+ " accountNumber : " + exchange.getProperty("accountNumber", String.class) + " and nickNameID is "
					+ exchange.getProperty("nickNameID", String.class) + " and response is " + response.toString(), e);
			e.printStackTrace();
		}
	}

}
