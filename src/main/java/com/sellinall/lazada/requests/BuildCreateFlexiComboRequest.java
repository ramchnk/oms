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

public class BuildCreateFlexiComboRequest implements Processor {
	static Logger log = Logger.getLogger(BuildCreateFlexiComboRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		callCreateFlexiComboAPI(exchange);
	}

	private void callCreateFlexiComboAPI(Exchange exchange) throws IOException, JSONException {
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		String promotionName = exchange.getProperty("promotionName", String.class);
		String discountType = exchange.getProperty("discountType", String.class);
		String discountApplyFor = exchange.getProperty("discountApplyFor", String.class);
		if (discountApplyFor.equalsIgnoreCase("specificproducts")) {
			discountApplyFor = "SPECIFIC_PRODUCTS";
		} else {
			discountApplyFor = "ENTIRE_STORE";
		}
		String criteriaType = exchange.getProperty("criteriaType", String.class);
		int maxOrders = exchange.getProperty("maxOrders", Integer.class);
		Long startDate = exchange.getProperty("startDate", Long.class);
		Long endDate = exchange.getProperty("endDate", Long.class);
		JSONArray criteriaValues = exchange.getProperty("criteriaValues", JSONArray.class);
		JSONArray discountValues = exchange.getProperty("discountValues", JSONArray.class);
		JSONArray sampleSKUList = new JSONArray();
		JSONArray freeGiftList = new JSONArray();
		if (discountType.contains("Sample")) {
			sampleSKUList = exchange.getProperty("sampleSKUList", JSONArray.class);
		}
		if (discountType.contains("Gift")) {
			freeGiftList = exchange.getProperty("freeGiftList", JSONArray.class);
		}
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("apply", discountApplyFor);
		map.put("name", promotionName);
		map.put("criteria_type", criteriaType.toUpperCase());
		map.put("order_numbers", maxOrders + "");
		map.put("start_time", startDate + "");
		map.put("end_time", endDate + "");
		map.put("discount_type", discountType);
		map.put("criteria_value", criteriaValues.toString());
		if (discountValues.length() > 0)
			map.put("discount_value", discountValues.toString());
		if (sampleSKUList.length() > 0)
			map.put("sample_skus", sampleSKUList.toString());
		if (freeGiftList.length() > 0)
			map.put("gift_skus", freeGiftList.toString());
		map.put("access_token", accessToken);
		String queryParams = "";
		queryParams += "&apply=" + discountApplyFor;
		queryParams += "&name=" + URLEncoder.encode(promotionName, "UTF-8");
		queryParams += "&criteria_type=" + criteriaType.toUpperCase();
		queryParams += "&order_numbers=" + maxOrders;
		queryParams += "&start_time=" + startDate;
		queryParams += "&end_time=" + endDate;
		queryParams += "&discount_type=" + discountType;
		queryParams += "&criteria_value=" + URLEncoder.encode(criteriaValues.toString(), "UTF-8");
		if (discountValues.length() > 0)
			queryParams += "&discount_value=" + URLEncoder.encode(discountValues.toString(), "UTF-8");
		if (sampleSKUList.length() > 0)
			queryParams += "&sample_skus=" + URLEncoder.encode(sampleSKUList.toString(), "UTF-8");
		if (freeGiftList.length() > 0)
			queryParams += "&gift_skus=" + URLEncoder.encode(freeGiftList.toString(), "UTF-8");
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = "";
		try {
			response = NewLazadaConnectionUtil.callAPI(hostURL, "/promotion/flexicombo/create", accessToken, map, "",
					queryParams, "POST", clientID, clientSecret);
			JSONObject serviceResponse = new JSONObject(response);
			if (serviceResponse.has("code") && serviceResponse.getString("code").equals("0")) {
				exchange.setProperty("isPromotionCreated", true);
				String promotionID = serviceResponse.getString("data");
				exchange.setProperty("promotionID", promotionID);
				log.info("PromotionID created for accountNumber  " + exchange.getProperty("accountNumber", String.class)
						+ " and promotionID : " + promotionID);
				return;
			}
			if (serviceResponse.has("message")) {
				exchange.setProperty("failureReason", serviceResponse.getString("message"));
			}
			log.error("Flexi combo creation failed for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " and nickNameID is "
					+ exchange.getProperty("nickNameID", String.class) + " and response is " + response.toString());
			exchange.setProperty("isPromotionCreated", false);
		} catch (Exception e) {
			log.error("Exception ocurred while creating flexi combo for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " and nickNameID is "
					+ exchange.getProperty("nickNameID", String.class) + " and response is " + response.toString(), e);
			exchange.setProperty("isPromotionCreated", false);
		}
	}

}