package com.sellinall.lazada.requests;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetItemDetails implements Processor {
	static Logger log = Logger.getLogger(GetItemDetails.class.getName());

	public void process(Exchange exchange) throws Exception {
		exchange.setProperty("responseSuccess", false);
		String accountNumber = "";
		if (exchange.getProperties().containsKey("accountNumber")) {
			accountNumber = (String) exchange.getProperty("accountNumber");
		}
		ArrayList<JSONObject> arrayList = (ArrayList<JSONObject>) exchange.getProperty("pulledInventoryList");
		JSONObject itemFromSite = arrayList.get(exchange.getProperty("itemListIndex", Integer.class));
		String parentItemId = itemFromSite.getString("item_id");
		exchange.setProperty("parentItemID", parentItemId);
		log.info("Current processing itemId: " + parentItemId);
		JSONObject itemDetails = getItemDetails(exchange, accountNumber, itemFromSite.getString("item_id"), 0);
		if (itemDetails == null) {
			return;
		}
		exchange.getOut().setBody(itemDetails);
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
		long startTime = System.currentTimeMillis();
		try {
			response = NewLazadaConnectionUtil.callAPI(hostUrl, apiName, accessToken, map, "", queryParam, "GET",
					clientID, clientSecret);
			if (response.equals("")) {
				long endTime = System.currentTimeMillis();
				log.error("Empty response for get item details for accountNumber : " + accountNumber
						+ " ,for nickNameID: " + exchange.getProperty("nickNameID") + ", took : "
						+ (endTime - startTime) + "ms");
				return null;
			}
			JSONObject channelResponse = new JSONObject(response);
			if (!channelResponse.has("data")) {
				if (channelResponse.has("code")) {
					String code = channelResponse.getString("code");
					if (code.contains("ApiCallLimit") && retryCount < 3) {
						Thread.sleep(10000);
						retryCount++;
						return getItemDetails(exchange, accountNumber, parentItemId, retryCount);
					}
				}
				log.error("Response for get item details " + response + ", for accountNumber : " + accountNumber
						+ " for nickNameID: " + exchange.getProperty("nickNameID"));
				return null;
			}
			exchange.setProperty("isInventoryPollingSuccess", true);
			return channelResponse.getJSONObject("data");
		} catch (Exception e) {
			log.error("Internal error occured for get item details for itemId - " + parentItemId
					+ " ,for accountNumber: " + accountNumber + " ,for nickNameID: "
					+ exchange.getProperty("nickNameID") + " and response: " + response);
			e.printStackTrace();
		}
		return null;
	}
}
