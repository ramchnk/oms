package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetBrand implements Processor {

	/*
	 * this method is used to construct the parameters for the lazada api
	 * consumption. the api returns the list of brands that gets returned
	 * through camel using exchange.
	 */
	public void process(Exchange exchange) throws Exception {

		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("offset", exchange.getProperty("offset", String.class));
		map.put("limit", exchange.getProperty("limit", String.class));
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String queryParams = "&offset=" + exchange.getProperty("offset", String.class) + "&limit="
				+ exchange.getProperty("limit", String.class);
		String response = NewLazadaConnectionUtil.callAPI(hostURL, "/brands/get", accessToken, map, "", queryParams,
				"GET", clientID, clientSecret);
		JSONObject responseData = new JSONObject();
		responseData.put("data", new JSONObject(response));
		exchange.getOut().setBody(responseData);
	}
}
