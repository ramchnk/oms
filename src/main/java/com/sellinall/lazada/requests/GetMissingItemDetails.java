package com.sellinall.lazada.requests;

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

public class GetMissingItemDetails implements Processor {
	static Logger log = Logger.getLogger(GetMissingItemDetails.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		exchange.setProperty("responseSuccess", false);
		String accountNumber = (String) exchange.getProperty("accountNumber");
		String nickNameID = (String) exchange.getProperty("nickNameID");
		ArrayList<String> missingItemIdList = (ArrayList<String>) exchange.getProperty("missingItemIdList");
		int index = exchange.getProperty("itemListIndex", Integer.class);
		String parentItemId = missingItemIdList.get(index);
		exchange.setProperty("parentItemID", parentItemId);
		JSONObject itemDetails = getItemDetails(exchange, accountNumber, parentItemId, 0);
		if (itemDetails == null) {
			exchange.setProperty("isImportMissingItem", false);
			return;
		}
		exchange.setProperty("item", itemDetails);
		JSONArray skus = new JSONArray();
		if (itemDetails.has("skus")) {
			skus = (JSONArray) itemDetails.get("skus");
		} else if (itemDetails.has("Skus")) {
			skus = (JSONArray) itemDetails.get("Skus");
		}
		if (skus.length() < 1) {
			log.error("No more item found for ItemId : " + exchange.getProperty("parentItemID", String.class)
					+ ",  account : " + accountNumber + ", nickNameID: " + nickNameID + " and itemFromSite : "
					+ itemDetails.toString());
			return;
		}
		exchange.setProperty("hasItemExists", true);
		String itemStatus = skus.getJSONObject(0).getString("Status");
		exchange.setProperty("itemStatus", itemStatus);
		String customSKU = skus.getJSONObject(0).getString("SellerSku");
		exchange.setProperty("SellerSku", customSKU);
		exchange.setProperty("isInventoryEmpty", false);
		log.info("currently processing customSKU : " + customSKU + ", with status : " + itemStatus + ", for account : "
				+ accountNumber + ", for nickNameID: " + nickNameID);
		exchange.setProperty("responseSuccess", true);

	}

	private JSONObject getItemDetails(Exchange exchange, String accountNumber, String parentItemId, int retryCount) {
		HashMap<String, String> map = new HashMap<String, String>();
		exchange.setProperty("isInventoryPollingSuccess", false);
		String response = "";
		String queryParam = "";
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostUrl = exchange.getProperty("hostURL", String.class);
		map.put("access_token", accessToken);
		queryParam += "&item_id=" + parentItemId;
		map.put("item_id", parentItemId);
		String apiName = "/product/item/get";
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		try {
			response = NewLazadaConnectionUtil.callAPI(hostUrl, apiName, accessToken, map, "", queryParam, "GET",
					clientID, clientSecret);
			if (response.isEmpty()) {
				log.error("Empty response for get missing item details for accountNumber : " + accountNumber
						+ " ,for nickNameID: " + exchange.getProperty("nickNameID"));
				return null;
			}
			JSONObject channelResponse = new JSONObject(response);
			if (!channelResponse.has("data")) {
				if (channelResponse.has("code")) {
					String code = channelResponse.getString("code");
					if (code.contains("ApiCallLimit") && retryCount < 3) {
						Thread.sleep(3000);
						retryCount++;
						return getItemDetails(exchange, accountNumber, parentItemId, retryCount);
					}
				}
				log.error("Response for get item details " + response + ", for accountNumber : " + accountNumber
						+ " for nickNameID: " + exchange.getProperty("nickNameID"));
				return null;
			}
			return channelResponse.getJSONObject("data");
		} catch (Exception e) {
			log.error("Internal error occured for get missing item details for itemId - " + parentItemId
					+ " ,for accountNumber: " + accountNumber + " ,for nickNameID: "
					+ exchange.getProperty("nickNameID") + " and response: " + response);
			e.printStackTrace();
		}
		return null;
	}

}
