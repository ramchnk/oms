package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

/**
 * 
 * @author Raguvaran.S
 *
 */

public class GetCategorySuggestion implements Processor {
	static Logger log = Logger.getLogger(GetCategorySuggestion.class.getName());

	public void process(Exchange exchange) throws Exception {

		String itemTitle = exchange.getProperty("itemTitle", String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("product_name", itemTitle);
		String params = "&product_name=" + URLEncoder.encode(itemTitle);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String responseString = NewLazadaConnectionUtil.callAPI(hostURL, "/product/category/suggestion/get",
				accessToken, map, "", params, "GET", clientID, clientSecret);
		JSONObject payload = new JSONObject(responseString);
		JSONObject response = new JSONObject();
		if (payload.has("code") && payload.getString("code").equals("0")) {
			response.put("response", "success");
			response.put("payload", responseString);
		} else {
			response.put("response", "failure");
			log.error("Error occurred while getting category suggestion for nickNameId: "
					+ exchange.getProperty("nickNameID", String.class) + " and accountNumber: "
					+ exchange.getProperty("accountNumber", String.class) + " response: " + responseString);
		}
		exchange.getOut().setBody(response);
	}

}
