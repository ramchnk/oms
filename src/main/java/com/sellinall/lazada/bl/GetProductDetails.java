package com.sellinall.lazada.bl;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.log4j.Logger;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetProductDetails {
	static Logger log = Logger.getLogger(GetProductDetails.class.getName());

	public static String getProductItems(Exchange exchange, String itemId, String customSKU) {
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostUrl = exchange.getProperty("hostURL", String.class);
		String response = "";

		String queryParam = "";
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		if (!itemId.isEmpty()) {
			queryParam = "&item_id=" + itemId;
			map.put("item_id", itemId);
		}
		if (!customSKU.isEmpty()) {
			queryParam = "&seller_sku=" + customSKU;
			map.put("seller_sku", customSKU);
		}

		String apiName = "/product/item/get";
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		try {
			response = NewLazadaConnectionUtil.callAPI(hostUrl, apiName, accessToken, map, "", queryParam, "GET",
					clientID, clientSecret);
			return response;
		} catch (Exception e) {
			log.error("Failed to get item details for item_id : " + itemId + " & seller_sku: " + customSKU);
			e.printStackTrace();
		}
		return null;
	}

}
