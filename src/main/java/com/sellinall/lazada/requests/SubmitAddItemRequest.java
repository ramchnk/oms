package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class SubmitAddItemRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitAddItemRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		String payload = exchange.getIn().getBody(String.class);
		String response = null;
		exchange.setProperty("isEligibleToUpdatePM", false);
		if (payload.trim().equals("error")) {
			exchange.setProperty("isPostingSuccess", false);
			exchange.setProperty("failureReason", "Failure - Getting invalid Api response, Please try again after some time");
			return;
		}
		if (exchange.getProperties().containsKey("accessToken")) {
			String SKU = "";
			String requestType = exchange.getProperty("requestType", String.class);
			if (requestType.equals("addVariant")) {
				List<String> SKUList = exchange.getProperty("variantsList", List.class);
				SKU = SKUList.toString();
			} else if (exchange.getProperty("isStatusUpdate", Boolean.class)) {
				SKU = exchange.getProperty("SKU").toString();
			} else {
				SKU = exchange.getProperty("SKU").toString().split("-")[0];
			}
			String accessToken = exchange.getProperty("accessToken", String.class);
			HashMap<String, String> map = new HashMap<String, String>();
			String url = exchange.getProperty("hostURL", String.class);
			
			String apiName = getApiName(exchange);
			if (exchange.getProperties().containsKey("accountNumber")
					&& exchange.getProperty("accountNumber", String.class) != null && Config.getConfig()
							.getTestAccountNumber().equals(exchange.getProperty("accountNumber", String.class))) {
				log.info("added log for check api for accountNumber : "
						+ exchange.getProperty("accountNumber", String.class) + ", SKU : " + SKU + ", apiName is : "
						+ apiName + " and isImagesUpdate : " + exchange.getProperty("isImagesUpdate", boolean.class));
			}
			map.put("access_token", accessToken);
			map.put("payload", payload);
			String requestBody = "payload=" + URLEncoder.encode(payload);
			log.debug("Add item payLoad =" + payload);
			try {
				String clientID = Config.getConfig().getLazadaClientID();
				String clientSecret = Config.getConfig().getLazadaClientSecret();
				response = NewLazadaConnectionUtil.callAPI(url, apiName, accessToken, map, requestBody, "",
						"POST", clientID, clientSecret);
				if (exchange.getProperty("isStatusUpdate") != null
						&& exchange.getProperty("isStatusUpdate", Boolean.class)) {
					String sellerSKU = "";
					if (exchange.getProperty("refrenceID") != null) {
						sellerSKU = exchange.getProperty("refrenceID", String.class);
					}
					log.info("status update done for SKU: " + exchange.getProperty("SKU") + " , sellerSKU : "
							+ sellerSKU + " ,accountNumber : " + exchange.getProperty("accountNumber", String.class)
							+ " nickNameID : " + exchange.getProperty("nickNameID", String.class) + " and response : "
							+ response);
				}
				log.debug("Add Item Response : " + response);
				if (exchange.getProperties().containsKey("accountNumber")
						&& exchange.getProperty("accountNumber", String.class) != null && Config.getConfig()
								.getTestAccountNumber().equals(exchange.getProperty("accountNumber", String.class))) {
					log.info("Product update for accountNumber : " + exchange.getProperty("accountNumber", String.class)
							+ " and SKU : " + SKU + " and payload is : " + payload + " and response is : " + response);
				}
				JSONObject channelResponse = new JSONObject(response);
				if (channelResponse.has("code") && channelResponse.getString("code").equals("0")) {
					exchange.setProperty("isPostingSuccess", true);
					exchange.setProperty("isEligibleToUpdatePM", true);
					if (channelResponse.has("data") && channelResponse.get("data") instanceof JSONObject) {
						JSONObject data = channelResponse.getJSONObject("data");
						if (data.has("item_id")) {
							exchange.setProperty("itemID", data.getString("item_id"));
						}
						if (data.has("sku_list")) {
							JSONArray skuList = data.getJSONArray("sku_list");
							Map<String, String> customSKUAndSkuIdMap = new HashMap<String, String>();
							for (int i = 0; i < skuList.length(); i++) {
								JSONObject skuListObj = skuList.getJSONObject(i);
								customSKUAndSkuIdMap.put(skuListObj.getString("seller_sku"),
										skuListObj.getString("sku_id"));
							}
							exchange.setProperty("customSKUAndSkuIdMap", customSKUAndSkuIdMap);
						}
					}
					if (exchange.getProperties().containsKey("isSalePriceUpdate")
							&& exchange.getProperty("isSalePriceUpdate", Boolean.class)) {
						exchange.setProperty("isPromotionEnabled", true);
					}
				} else {
					exchange.setProperty("isPostingSuccess", false);
					log.error("Posting Failed for " + SKU + " and its response is: " + channelResponse);
					exchange.setProperty("failureReason", "Failure - " + getApiErrorMessage(channelResponse));
				}
			} catch (Exception e) {
				log.error("Exception occured while adding item for SKU: " + exchange.getProperty("SKU")
						+ " ,accountNumber : " + exchange.getProperty("accountNumber", String.class) + " nickNameID : "
						+ exchange.getProperty("nickNameID", String.class) + " and response : " + response);
				e.printStackTrace();
				exchange.setProperty("isPostingSuccess", false);
				exchange.setProperty("failureReason",
						"Failure - getting failure response from Lazada - " + e.getMessage());
			}
		} else {
			exchange.setProperty("isPostingSuccess", false);
			exchange.setProperty("failureReason", "Failure - access token not found");
		}
	}

	private String getApiErrorMessage(JSONObject channelResponse) {
		try {
			String errorMessage = "";
			if (channelResponse.has("detail")) {
				JSONArray details = channelResponse.getJSONArray("detail");
				for (int i = 0; i < details.length(); i++) {
					JSONObject detail = details.getJSONObject(i);
					if (detail.has("field")) {
						errorMessage += (errorMessage.isEmpty() ? "" : ",") + detail.getString("field") + ":"
								+ detail.getString("message");
					} else {
						errorMessage += (errorMessage.isEmpty() ? "" : ",") + detail.getString("message");
					}
				}
			}
			return channelResponse.getString("message") + (errorMessage.isEmpty() ? "" : "-") + errorMessage;
		} catch (Exception e) {
			log.error("Add item response:"+channelResponse);
		}
		return "Please check itemDescription and other fileds";
	}

	// Method will return either create product or update product
	private static String getApiName(Exchange exchange) {
		String requestType = exchange.getProperty("requestType", String.class);
		if (requestType.equals("batchEditItem") || exchange.getProperty("isStatusUpdate", boolean.class)
				|| exchange.getProperty("isImagesUpdate", boolean.class) || requestType.equals("editItem")) {
			return "/product/update";
		}
		return "/product/create";
	}
}