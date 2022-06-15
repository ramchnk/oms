package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class BuildGetDetailsRequest implements Processor {
	static Logger log = Logger.getLogger(BuildGetDetailsRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		String ScApiHost = exchange.getProperty("hostURL", String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String apiName = exchange.getProperty("apiName", String.class);
		String queryParams = "";
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		if (exchange.getProperty("orderID", String.class) != null) {
			String orderID = exchange.getProperty("orderID", String.class);
			map.put("order_id", orderID);
			queryParams = "&order_id=" + orderID;
		} else if (exchange.getProperty("itemID", String.class) != null) {
			String itemID = exchange.getProperty("itemID", String.class);
			map.put("item_id", itemID);
			queryParams = "&item_id=" + itemID;
		} else if (exchange.getProperty("sellerSKU", String.class) != null) {
			String sellerSKU = exchange.getProperty("sellerSKU", String.class);
			map.put("seller_sku", sellerSKU);
			queryParams = "&seller_sku=" + sellerSKU;
		}
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = NewLazadaConnectionUtil.callAPI(ScApiHost, apiName, accessToken, map, "", queryParams, "GET",
				clientID, clientSecret);
		exchange.getOut().setBody(response);
	}
}
