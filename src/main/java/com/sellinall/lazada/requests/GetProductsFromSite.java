package com.sellinall.lazada.requests;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
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

public class GetProductsFromSite implements Processor {

	static Logger log = Logger.getLogger(GetProductsFromSite.class.getName());

	public void process(Exchange exchange) throws Exception {
		String ScApiHost = exchange.getProperty("hostURL", String.class);
		Map<String, JSONObject> refrenceIDMap = exchange.getProperty("refrenceIDMap", HashMap.class);
		String filterValue = exchange.getProperty("inventoryFilter", String.class);
		List<String> refrenceIDList = exchange.getProperty("refrenceIDList", List.class);
		/*TODO: Need to remove below lines, once QC polling issue resolved*/
		/*if (filterValue.equals("all") && exchange.getProperties().containsKey("approvedRefrenceIDList")) {
			refrenceIDList = exchange.getProperty("approvedRefrenceIDList", ArrayList.class);
		} else if (filterValue.equals("deleted") && exchange.getProperties().containsKey("missedSKUinQCPolling")) {
			refrenceIDList = exchange.getProperty("missedSKUinQCPolling", ArrayList.class);
		}*/
		boolean isEligibleToUpdateQCStatus = false;
		if (exchange.getProperties().containsKey("isEligibleToUpdateQCStatus")) {
			isEligibleToUpdateQCStatus = exchange.getProperty("isEligibleToUpdateQCStatus", Boolean.class);
		}
		if (refrenceIDList.size() > 0) {
			isEligibleToUpdateQCStatus = true;
		}
		JSONArray referenceIDArray = new JSONArray(refrenceIDList);
		exchange.setProperty("isEligibleToUpdateQCStatus", isEligibleToUpdateQCStatus);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("filter", filterValue);
		map.put("sku_seller_list", referenceIDArray.toString());
		String queryParam = "&filter=" + filterValue + "&sku_seller_list="
				+ URLEncoder.encode(referenceIDArray.toString(), "UTF-8");
		JSONArray products = callApi(ScApiHost, queryParam, map, exchange.getProperty("accessToken", String.class),
				exchange);
		if (products == null) {
			return;
		}
		JSONArray skus = new JSONArray();
		JSONArray customSKUListToPushProductMaster = new JSONArray();
		JSONArray customSKUListToPullProductMaster = new JSONArray();
		for (int i = 0; i < products.length(); i++) {
			JSONObject channelItem = products.getJSONObject(i);
			if (channelItem.has("skus")) {
				skus = channelItem.getJSONArray("skus");
			} else {
				skus = channelItem.getJSONArray("Skus");
			}
			for (int j = 0; j < skus.length(); j++) {
				String sku = skus.getJSONObject(j).getString("SellerSku");
				if (refrenceIDMap.containsKey(sku)) {
					JSONObject refrenceIDObj = refrenceIDMap.get(sku);
					String status = skus.getJSONObject(j).getString("Status");
					refrenceIDObj.put("status", status);
					updatePushPullList(status, customSKUListToPushProductMaster, customSKUListToPullProductMaster, sku,
							exchange);
					if (skus.getJSONObject(j).has("Url")) {
						refrenceIDObj.put("itemURL", skus.getJSONObject(j).getString("Url"));
					}
					if (skus.getJSONObject(j).has("ShopSku")) {
						refrenceIDObj.put("shopSKU", skus.getJSONObject(j).getString("ShopSku"));
					}
					refrenceIDMap.put(sku, refrenceIDObj);
				} else if (refrenceIDList.contains(sku)) {
					/*deleted status inventories handled here*/
					JSONObject refrenceIDObj = new JSONObject();
					String status = skus.getJSONObject(j).getString("Status");
					refrenceIDObj.put("status", status);
					updatePushPullList(status, customSKUListToPushProductMaster, customSKUListToPullProductMaster, sku,
							exchange);
					refrenceIDObj.put("sellerSKU", sku);
					/*TODO : temp fix, once qc issue resolved, need to remove below changes*/
					if (skus.getJSONObject(j).has("Url")) {
						refrenceIDObj.put("itemURL", skus.getJSONObject(j).getString("Url"));
					}
					if (skus.getJSONObject(j).has("ShopSku")) {
						refrenceIDObj.put("shopSKU", skus.getJSONObject(j).getString("ShopSku"));
					}
					refrenceIDMap.put(sku, refrenceIDObj);
				}
			}
		}
		boolean isEligibleToUpdateProductMaster = false;
		if (customSKUListToPushProductMaster.length() > 0 || customSKUListToPullProductMaster.length() > 0) {
			isEligibleToUpdateProductMaster = true;
			exchange.setProperty("customSKUListToPushProductMaster", customSKUListToPushProductMaster);
			exchange.setProperty("customSKUListToPullProductMaster", customSKUListToPullProductMaster);
		}
		exchange.setProperty("isEligibleToUpdateProductMaster", isEligibleToUpdateProductMaster);
		ArrayList<JSONObject> refrenceIDObjList = new ArrayList<JSONObject>(refrenceIDMap.values());
		exchange.getOut().setBody(refrenceIDObjList);
	}

	private void updatePushPullList(String status, JSONArray customSKUListToPushProductMaster,
			JSONArray customSKUListToPullProductMaster, String sku, Exchange exchange) {
		if (status.equals("active")) {
			customSKUListToPushProductMaster.put(sku);
		} else if (status.equals("inactive") || status.equals("deleted")) {
			customSKUListToPullProductMaster.put(sku);
		}
	}

	private JSONArray callApi(String scApiHost, String queryParam, HashMap<String, String> map, String accessToken,
			Exchange exchange) throws IOException {
		String accountNumber = "";
		if (exchange.getProperties().containsKey("accountNumber")) {
			accountNumber = (String) exchange.getProperty("accountNumber");
		}
		String response = "";
		map.put("access_token", accessToken);
		String apiName = "/products/get";
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		try {
			response = NewLazadaConnectionUtil.callAPI(scApiHost, apiName, accessToken, map, "", queryParam, "GET",
					clientID, clientSecret);
			JSONObject channelResponse = new JSONObject(response);
			if (!channelResponse.has("data") || channelResponse.getJSONObject("data").length() == 0) {
				exchange.setProperty("failureReason", channelResponse);
				log.info("No item found for accountNumber : " + accountNumber + ", nickNameID : "
						+ exchange.getProperty("nickNameID", String.class) + " & response " + response);
				return null;
			}
			JSONObject data = channelResponse.getJSONObject("data");
			return data.getJSONArray("products");
		} catch (Exception e) {
			log.error("Error occurred while getting products for accountNumber : " + accountNumber + ", nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " response is: " + response);
			e.printStackTrace();
		}
		return null;
	}
}
