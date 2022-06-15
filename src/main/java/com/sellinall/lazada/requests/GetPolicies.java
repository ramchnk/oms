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

public class GetPolicies implements Processor {
	static Logger log = Logger.getLogger(GetPolicies.class.getName());

	public void process(Exchange exchange) throws Exception {
		log.debug("Inside ProcessSyncPolicies");
		ArrayList<DBObject> shippingProviders = new ArrayList<DBObject>();
		if (exchange.getProperties().containsKey("accessToken")) {
			String accessToken = exchange.getProperty("accessToken", String.class);
			String hostURL = exchange.getProperty("hostURL", String.class);
			shippingProviders = GetShippingProviderDetails(accessToken, hostURL);
		}
		if (shippingProviders != null) {
			exchange.setProperty("isPolicyAvailable", true);
			exchange.getOut().setBody(shippingProviders);
			return;
		}
		JSONObject response = new JSONObject();
		response.put("response", "failure");
		exchange.setProperty("isPolicyAvailable", false);
		exchange.getOut().setBody(response);
	}

	public static ArrayList<DBObject> GetShippingProviderDetails(String accessToken, String hostURL)
			throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		ArrayList<DBObject> shippingProvider = new ArrayList<DBObject>();
		map.put("access_token", accessToken);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = NewLazadaConnectionUtil.callAPI(hostURL, "/shipment/providers/get", accessToken, map, "", "",
				"GET", clientID, clientSecret);
		JSONObject shippingServiceProvider = new JSONObject(response);
		if (shippingServiceProvider.getString("code").equals("0")) {
			JSONObject shipmentDetails = shippingServiceProvider.getJSONObject("data");
			JSONArray shipment_providers = shipmentDetails.getJSONArray("shipment_providers");
			for (int i = 0; i < shipment_providers.length(); i++) {
				DBObject shippingDetail = (DBObject) JSON.parse(shipment_providers.get(i).toString());
				shippingDetail.put("preferred", false);
				shippingProvider.add(shippingDetail);
			}
			return shippingProvider;
		}
		log.error("Get Shipping Policy Response:"+shippingServiceProvider);
		return null;
	}
}
