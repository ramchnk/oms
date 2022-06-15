package com.sellinall.lazada.requests;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetDeliveryOptions implements Processor {
	static Logger log = Logger.getLogger(GetDeliveryOptions.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject userChannel = exchange.getProperty("userChannel", JSONObject.class);
		ArrayList<DBObject> shippingDeliveryList = null;
		if (exchange.getProperties().containsKey("accessToken")) {
			String accessToken = exchange.getProperty("accessToken", String.class);
			JSONObject postHelper = userChannel.getJSONObject("postHelper");
			shippingDeliveryList = GetShippingDeliveryOptions(accessToken, postHelper.getString("hostURL"));
		}
		if (shippingDeliveryList != null) {
			exchange.setProperty("isDeliveryOptionAvailable", true);
			exchange.getOut().setBody(shippingDeliveryList);
			return;
		}
		JSONObject response = new JSONObject();
		response.put("response", "failure");
		exchange.setProperty("isDeliveryOptionAvailable", false);
		exchange.getOut().setBody(response);
	}

	public static ArrayList<DBObject> GetShippingDeliveryOptions(String accessToken, String hostURL) throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		ArrayList<DBObject> shippingDeliveryList = new ArrayList<DBObject>();
		map.put("access_token", accessToken);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = "";
		try {
			response = NewLazadaConnectionUtil.callAPI(hostURL, "/promotion/freeshipping/deliveryoptions/get",
					accessToken, map, "", "", "GET", clientID, clientSecret);
			JSONObject shippingdeliveryOptions = new JSONObject(response);
			if (shippingdeliveryOptions.getString("code").equals("0")) {
				JSONArray data = shippingdeliveryOptions.getJSONArray("data");
				for (int i = 0; i < data.length(); i++) {
					DBObject shippingDeliveryObj = (DBObject) JSON.parse(data.get(i).toString());
					shippingDeliveryList.add(shippingDeliveryObj);
				}
				return shippingDeliveryList;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.error("Getting failure response in shipping delivery options :" + response);
		return null;
	}
}
