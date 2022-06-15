package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetCategoryAttributes implements Processor {
	static Logger log = Logger.getLogger(GetCategoryAttributes.class.getName());

	public void process(Exchange exchange) throws Exception {

		String categoryID = exchange.getProperty("categoryID", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("primary_category_id", categoryID);
		String params = "&primary_category_id=" + categoryID;
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = NewLazadaConnectionUtil.callAPI(hostURL, "/category/attributes/get", null, map, "", params,
				"GET", clientID, clientSecret);
		exchange.getOut().setBody(response);
	}

}
