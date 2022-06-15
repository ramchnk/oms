package com.sellinall.lazada.validation;

import java.util.HashMap;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class ValidateStockQuantity implements Processor {
	static Logger log = Logger.getLogger(ValidateStockQuantity.class.getName());

	public void process(Exchange exchange) throws Exception {
		getItemDetails(exchange, 0);
	}

	private void getItemDetails(Exchange exchange, int retryCount) {
		HashMap<String, String> map = new HashMap<String, String>();
		String response = "";
		String parentItemId = "";
		String sellerSku = "";
		String queryParam = "";
		if (exchange.getProperties().containsKey("sellerSKU")) {
			sellerSku = exchange.getProperty("sellerSKU", String.class);
		}
		if (exchange.getProperties().containsKey("itemID")) {
			parentItemId = exchange.getProperty("itemID", String.class);
			queryParam = "&item_id=" + parentItemId;
			map.put("item_id", parentItemId);
		} else if (sellerSku != "") {
			queryParam = "&seller_sku=" + sellerSku;
			map.put("seller_sku", sellerSku);
		} else {
			log.error("Need sellerSku or itemId to get itemDetails " + "for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " for nickNameID: "
					+ exchange.getProperty("nickNameID") + " request payload : "
					+ exchange.getProperty("request", JSONObject.class));
			return;
		}
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostUrl = exchange.getProperty("hostURL", String.class);
		map.put("access_token", accessToken);
		String apiName = "/product/item/get";
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		try {
			response = NewLazadaConnectionUtil.callAPI(hostUrl, apiName, accessToken, map, "", queryParam, "GET",
					clientID, clientSecret);
			if (!response.startsWith("{")) {
				log.error("Invalid response for accountNumber : " + exchange.getProperty("accountNumber", String.class)
						+ " for nickNameID: " + exchange.getProperty("nickNameID")
						+ (!parentItemId.isEmpty() ? " for itemId:" + parentItemId : " for sellerSku:" + sellerSku)
						+ " response is:" + response.toString());
				return;
			}
			JSONObject channelResponse = new JSONObject(response);
			if (!channelResponse.has("data")
					|| (channelResponse.has("code") && !channelResponse.getString("code").equals("0"))) {
				log.error("Invalid response for accountNumber : " + exchange.getProperty("accountNumber", String.class)
						+ " for nickNameID: " + exchange.getProperty("nickNameID")
						+ (!parentItemId.isEmpty() ? " for itemId:" + parentItemId : " for sellerSku:" + sellerSku)
						+ " response is:" + response.toString());
				return;
			}
			boolean isStockUpdateSuccess = true;
			if (channelResponse.getJSONObject("data").has("skus")) {
				JSONArray skus = channelResponse.getJSONObject("data").getJSONArray("skus");
				int requestQuantity = 0, responseQuantity = 0;
				if (exchange.getProperties().containsKey("quantity")) {
					requestQuantity = exchange.getProperty("quantity", Integer.class);
				}
				for (int i = 0; i < skus.length(); i++) {
					JSONObject sku = skus.getJSONObject(i);
					if (sku.has("SellerSku") && sku.get("SellerSku").equals(sellerSku)) {
						responseQuantity = sku.getInt("quantity");
						break;
					}
				}
				if (requestQuantity != responseQuantity) {
					exchange.setProperty("failureReason", "Stock still not updated for this item");
					isStockUpdateSuccess = false;
					log.error("Stock still not updated " 
							+ " for itemId - " + parentItemId + " for sellerSku : " + sellerSku
							+ " ,for accountNumber: " + exchange.getProperty("accountNumber", String.class)
							+ " ,for nickNameID: " + exchange.getProperty("nickNameID") + " requestQuantity is : "
							+ requestQuantity + " quantityFromMarketplace is : " + responseQuantity + " updated stock response is : " 
							+ exchange.getProperty("stockUpdateResponse"));
				}
			}
			exchange.setProperty("isStockUpdateSuccess", isStockUpdateSuccess);
		} catch (Exception e) {
			log.error("Internal error occured for get item details "
					+ (!parentItemId.isEmpty() ? "for itemId - " + parentItemId : "for sellerSku:" + sellerSku)
					+ " ,for accountNumber: " + exchange.getProperty("accountNumber", String.class)
					+ " ,for nickNameID: " + exchange.getProperty("nickNameID") + " and response: " + response);
			e.printStackTrace();
		}
	}
}