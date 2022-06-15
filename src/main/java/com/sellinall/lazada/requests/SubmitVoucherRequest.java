package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class SubmitVoucherRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitVoucherRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accessToken = exchange.getProperty("accessToken", String.class);
		String criteriaOverMoney = exchange.getProperty("criteriaOverMoney", String.class);
		String promotionName = exchange.getProperty("promotionName", String.class);
		String voucherType = exchange.getProperty("voucherType", String.class);
		String voucherApplyFor = exchange.getProperty("voucherApplyFor", String.class);
		String voucherDisplayArea = exchange.getProperty("voucherDisplayArea", String.class);
		String voucherDiscountType = exchange.getProperty("voucherDiscountType", String.class);
		Long startDate = exchange.getProperty("startDate", Long.class);
		Long endDate = exchange.getProperty("endDate", Long.class);
		int voucherLimitPerCustomer = exchange.getProperty("voucherLimitPerCustomer", Integer.class);
		int issued = exchange.getProperty("issued", Integer.class);

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("voucher_name", promotionName);
		map.put("voucher_type", voucherType);
		map.put("criteria_over_money", criteriaOverMoney);
		map.put("apply", voucherApplyFor);
		map.put("display_area", voucherDisplayArea);
		map.put("period_start_time", String.valueOf(startDate * 1000L));
		map.put("period_end_time", String.valueOf(endDate * 1000L));
		map.put("voucher_discount_type", voucherDiscountType);
		map.put("limit", String.valueOf(voucherLimitPerCustomer));
		map.put("issued", String.valueOf(issued));

		String param = "&criteria_over_money=" + criteriaOverMoney + "&voucher_type=" + voucherType + "&apply="
				+ voucherApplyFor + "&display_area=" + voucherDisplayArea + "&period_end_time=" + (endDate * 1000L)
				+ "&voucher_name=" + URLEncoder.encode(promotionName, "UTF-8") + "&voucher_discount_type="
				+ voucherDiscountType + "&period_start_time=" + (startDate * 1000L) + "&limit="
				+ voucherLimitPerCustomer + "&issued=" + issued;

		if (exchange.getProperties().containsKey("promotionID")) {
			String promotionID = exchange.getProperty("promotionID", String.class);
			map.put("id", promotionID);
			param += "&id=" + promotionID;
		}
		if (exchange.getProperties().containsKey("offeringMoneyValueOff")) {
			String offeringMoneyValueOff = exchange.getProperty("offeringMoneyValueOff", String.class);
			map.put("offering_money_value_off", offeringMoneyValueOff);
			param += "&offering_money_value_off=" + offeringMoneyValueOff;
		}
		if (exchange.getProperties().containsKey("voucherCollectDate")) {
			long voucherCollectDate = exchange.getProperty("voucherCollectDate", Long.class);
			map.put("collect_start", String.valueOf(voucherCollectDate * 1000L));
			param += "&collect_start=" + (voucherCollectDate * 1000L);
		}
		if (exchange.getProperties().containsKey("maxDiscountOfferingMoneyValue")) {
			String maxDiscountOfferingMoneyValue = exchange.getProperty("maxDiscountOfferingMoneyValue", String.class);
			map.put("max_discount_offering_money_value", maxDiscountOfferingMoneyValue);
			param += "&max_discount_offering_money_value=" + maxDiscountOfferingMoneyValue;
		}
		if (exchange.getProperties().containsKey("offeringPercentageDiscountOff")) {
			int offering_percentage_discount_off = exchange.getProperty("offeringPercentageDiscountOff", Integer.class);
			map.put("offering_percentage_discount_off", String.valueOf(offering_percentage_discount_off));
			param += "&offering_percentage_discount_off=" + offering_percentage_discount_off;
		}
		String apiName = null;
		if (exchange.getProperty("action", String.class).equals("createPromotion")) {
			apiName = "/promotion/voucher/create";
		} else if (exchange.getProperty("action", String.class).equals("updatePromotion")) {
			apiName = "/promotion/voucher/update";
		}
		int retryCount = 1;
		if (apiName != null) {
			String promotionID = callAPI(exchange, apiName, "POST", accessToken, map, param, retryCount);
			if (!promotionID.isEmpty()) {
				exchange.setProperty("promotionID", promotionID);
				exchange.setProperty("isPromotionCreated", true);
			} else {
				exchange.setProperty("isPromotionCreated", false);
				// Note: If promotion update is failed, then need to remove added skus from DB
				exchange.setProperty("addFailedSKUs", exchange.getProperty("addedPromotionItems", List.class));
				exchange.setProperty("isAddPromotionItemFlow", true);
			}
		}

	}

	private String callAPI(Exchange exchange, String apiName, String requestType, String accessToken,
			HashMap<String, String> map, String queryParams, int retryCount)
			throws JSONException, InterruptedException {
		String url = exchange.getProperty("hostURL", String.class);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = NewLazadaConnectionUtil.callAPI(url, apiName, accessToken, map, "", queryParams, requestType,
				clientID, clientSecret);

		if (response.isEmpty() || !response.startsWith("{")) {
			log.error("Invalid voucher response getting for accountNumber: " + exchange.getProperty("accountNumber")
					+ ", nickNameId: " + exchange.getProperty("nickNameID") + ", requestID: "
					+ exchange.getProperty("requestID", String.class) + ", retry count :" + retryCount
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
				log.error("Error occuring when create voucher for accountNumber : "
						+ exchange.getProperty("accountNumber") + ", requestID: "
						+ exchange.getProperty("requestID", String.class) + ", nickNameID : "
						+ exchange.getProperty("nickNameID") + ", retry count :" + retryCount + " and response is: "
						+ response);
				if (retryCount <= 3) {
					retryCount++;
					Thread.sleep(1000);
					return callAPI(exchange, apiName, requestType, accessToken, map, queryParams, retryCount);
				}
				return "";
			}
			exchange.removeProperties("failureReason");
			return responseObject.getString("data");
		}
	}

}
