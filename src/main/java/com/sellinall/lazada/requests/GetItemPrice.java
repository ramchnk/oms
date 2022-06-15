package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.APIUrlConfig;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.DateUtil;

public class GetItemPrice implements Processor {
	static Logger log = Logger.getLogger(GetItemPrice.class.getName());

	public void process(Exchange exchange) throws Exception {
		String countryCode = exchange.getProperty("countryCode", String.class);
		String timeZone = LazadaUtil.timeZoneCountryMap.get(countryCode);
		String url = APIUrlConfig.getNewAPIUrl(countryCode);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String itemID = exchange.getProperty("itemID", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("item_id", itemID);
		map.put("access_token", accessToken);
		String param = "&item_id=" + itemID;
		JSONObject payload = new JSONObject();
		payload.put("item_id", itemID);
		String clientID = Config.getConfig().getCommonClientID();
		String clientSecret = Config.getConfig().getCommonClientSecret();
		String response = null;
		JSONObject responseData = new JSONObject();
		try {
			response = NewLazadaConnectionUtil.callAPI(url, "/product/item/get", accessToken, map, "", param, "GET",
					clientID, clientSecret);
			JSONObject result = new JSONObject(response);
			JSONObject data = result.getJSONObject("data");
			JSONArray skus = data.getJSONArray("skus");
			JSONObject skuData = skus.getJSONObject(0);
			String itemAmount = "";
			if (skuData.has("special_price") && skuData.has("special_from_time") && skuData.has("special_to_time")) {
				long startTime = DateUtil.getUnixTimestamp(skuData.getString("special_from_time"), "yyyy-MM-dd HH:mm",
						timeZone);
				long endTime = DateUtil.getUnixTimestamp(skuData.getString("special_to_time"), "yyyy-MM-dd HH:mm",
						timeZone);
				long currentTime = System.currentTimeMillis() / 1000;
				if (currentTime > startTime && currentTime < endTime) {
					itemAmount = skuData.getString("special_price");
				}
			}
			if (itemAmount.isEmpty()) {
				itemAmount = skuData.getString("price");
			}
			long priceInSiaFormat = CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(itemAmount));
			responseData.put("status", "success");
			responseData.put("itemAmount", CurrencyUtil.getJSONAmountObject(priceInSiaFormat,
					LazadaUtil.countryCodeToCurrencyMap.get(countryCode)));
		} catch (Exception e) {
			responseData.put("status", "failure");
			log.error("Error occurred while getting itemPrice for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + ", nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " response is: " + response);
			e.printStackTrace();
		}
		exchange.getOut().setBody(responseData);
	}
}
