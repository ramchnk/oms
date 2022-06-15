package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetCategory implements Processor {

	public void process(Exchange exchange) throws Exception {

		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = NewLazadaConnectionUtil.callAPI(hostURL, "/category/tree/get", accessToken, map, "", "",
				"GET", clientID, clientSecret);
		exchange.getOut().setBody(response);

	}

}
