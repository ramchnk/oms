package com.sellinall.lazada.requests;

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

public class SubmitFreeShippingRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitFreeShippingRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accessToken = exchange.getProperty("accessToken", String.class);
		String budgetType = exchange.getProperty("budgetType", String.class);
		String promotionName = exchange.getProperty("promotionName", String.class);
		String templateType = null;
		if (exchange.getProperties().containsKey("templateType")) {
			templateType = exchange.getProperty("templateType", String.class);
		}
		String discountApplyFor = exchange.getProperty("discountApplyFor", String.class);
		String templateCode = null;
		if (exchange.getProperties().containsKey("templateCode")) {
			templateCode = exchange.getProperty("templateCode", String.class);
		}
		String categoryName = null;
		if (exchange.getProperties().containsKey("categoryName")) {
			categoryName = exchange.getProperty("categoryName", String.class);
		}
		String budgetValue = null;
		if (exchange.getProperties().containsKey("budgetValue")) {
			budgetValue = exchange.getProperty("budgetValue", String.class);
		}
		String periodType = exchange.getProperty("periodType", String.class);
		String regionType = exchange.getProperty("regionType", String.class);
		Long startDate =  null;
		if (exchange.getProperties().containsKey("startDate")) {
			startDate = exchange.getProperty("startDate", Long.class);
		}
		Long endDate =  null;
		if (exchange.getProperties().containsKey("endDate")) {
			endDate = exchange.getProperty("endDate", Long.class);
		}
		String platformChannel = null;
		if (exchange.getProperties().containsKey("platformChannel")) {
			platformChannel = exchange.getProperty("platformChannel", String.class);
		}
		String campaignTag = null;
		if (exchange.getProperties().containsKey("campaignTag")) {
			campaignTag = exchange.getProperty("campaignTag", String.class);
		}
		JSONArray regionValue = null;
		if (exchange.getProperties().containsKey("regionValue")) {
			regionValue = exchange.getProperty("regionValue", JSONArray.class);
		}
		String deliveryOption = exchange.getProperty("deliveryOption", String.class);
		JSONArray tiers = null;
		if (exchange.getProperties().containsKey("tiers")) {
			tiers = exchange.getProperty("tiers", JSONArray.class);
		}
		String discountType = exchange.getProperty("discountType", String.class);
		String dealCriteria = exchange.getProperty("dealCriteria", String.class);

		String param = "&budget_type=" + budgetType + "&apply=" + discountApplyFor + "&promotion_name="
				+ URLEncoder.encode(promotionName, "UTF-8") + "&period_type=" + periodType + "&region_type="
				+ regionType + "&delivery_option=" + deliveryOption + "&discount_type=" + discountType
				+ "&deal_criteria=" + dealCriteria;

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("budget_type", budgetType);
		map.put("apply", discountApplyFor);
		map.put("promotion_name", promotionName);
		map.put("period_type", periodType);
		map.put("region_type", regionType);
		map.put("delivery_option", deliveryOption);
		map.put("discount_type", discountType);
		map.put("deal_criteria", dealCriteria);
		if (tiers != null) {
			param += "&tiers=" + URLEncoder.encode(tiers.toString(), "UTF-8");
			map.put("tiers", tiers.toString());
		}
		if (templateType != null) {
			param += "&template_type=" + templateType;
			map.put("template_type", templateType);
		}
		if (templateCode != null) {
			param += "&template_code=" + URLEncoder.encode(templateCode, "UTF-8");
			map.put("template_code", templateCode);
		}
		if (categoryName != null) {
			param += "&category_name=" + URLEncoder.encode(categoryName, "UTF-8");
			map.put("category_name", categoryName);
		}
		if (budgetValue != null) {
			param += "&budget_value=" + budgetValue;
			map.put("budget_value", budgetValue);
		}
		if (startDate != null) {
			param += "&period_start_time=" + (startDate * 1000L);
			map.put("period_start_time", String.valueOf(startDate * 1000L));
		}
		if (endDate != null) {
			param += "&period_end_time=" + (endDate * 1000L);
			map.put("period_end_time", String.valueOf(endDate * 1000L));
		}
		if (platformChannel != null) {
			param += "&platform_channel=" + URLEncoder.encode(platformChannel.toString(), "UTF-8");
			map.put("platform_channel", platformChannel);
		}
		if (campaignTag != null) {
			param += "&campaign_tag=" + URLEncoder.encode(campaignTag.toString(), "UTF-8");
			map.put("campaign_tag", campaignTag);
		}
		if (regionValue != null) {
			param += "&region_value=" + URLEncoder.encode(regionValue.toString(), "UTF-8");
			map.put("region_value", regionValue.toString());
		}
		String apiName = null;
		if (exchange.getProperty("action", String.class).equals("createPromotion")) {
			apiName = "/promotion/freeshipping/create";
		} else if (exchange.getProperty("action", String.class).equals("updatePromotion")) {
			apiName = "/promotion/freeshipping/update";
		}
		int retryCount = 1;
		if (apiName != null) {
			String promotionID = callAPI(exchange, apiName, "POST", accessToken, map, param, retryCount);
			if (!promotionID.isEmpty()) {
				exchange.setProperty("promotionID", promotionID);
				exchange.setProperty("isPromotionCreated", true);
			}
		}
	}

	private String callAPI(Exchange exchange, String apiName, String requestType, String accessToken,
			HashMap<String, String> map, String queryParams, int retryCount)
			throws JSONException, InterruptedException {
		String url = exchange.getProperty("hostURL", String.class);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = NewLazadaConnectionUtil.callAPI(url, apiName, accessToken, map, "", queryParams, requestType, clientID,
				clientSecret);
		if (response.isEmpty() || !response.startsWith("{")) {
			log.error("Invalid response getting for accountNumber: " + exchange.getProperty("accountNumber")
					+ ", nickNameId: " + exchange.getProperty("nickNameID") + ", retry count :" + retryCount
					+ " and the response is: " + response);
			exchange.setProperty("failureReason", "Getting empty response from lazada, please try again");
			if (retryCount <= 3) {
				retryCount++;
				Thread.sleep(1000);
				return callAPI(exchange, apiName, requestType, accessToken, map, queryParams, retryCount);
			}
			return "";
		} else {
			JSONObject responseObject = new JSONObject(response);
			if (!responseObject.getString("code").equals("0")) {
				if (responseObject.has("message")) {
					exchange.setProperty("failureReason", responseObject.getString("message"));
				}
				log.error("Error occuring when create freeShipping for accountNumber : "
						+ exchange.getProperty("accountNumber") + ", nickNameID : " + exchange.getProperty("nickNameID")
						+ ", retry count :" + retryCount + " and response is: " + response);
				if (retryCount <= 3) {
					retryCount++;
					Thread.sleep(1000);
					return callAPI(exchange, apiName, requestType, accessToken, map, queryParams, retryCount);
				}
				return "";
			}
			log.info("freeShipping created or updated for accountNumber : " + exchange.getProperty("accountNumber")
					+ ", nickNameID : " + exchange.getProperty("nickNameID") + ", retry count :" + retryCount
					+ " and response is: " + response);
			exchange.removeProperties("failureReason");
			return responseObject.getString("data");
		}
	}

}
