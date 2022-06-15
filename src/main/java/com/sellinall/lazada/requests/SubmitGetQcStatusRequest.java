package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class SubmitGetQcStatusRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitGetQcStatusRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		HashMap<String, String> params = new HashMap<String, String>();
		ArrayList<JSONObject> status = new ArrayList<JSONObject>();
		List<String> skuList = exchange.getProperty("refrenceIDList", List.class);
		exchange.getOut().setBody(status);
		String ScApiHost = exchange.getProperty("hostURL", String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		params.put("access_token", accessToken);
		params.put("seller_skus", skuList.toString());
		params.put("limit", "0");
		params.put("offset", "0");
		JSONArray responseArray = callApi(params, ScApiHost, accessToken, skuList, exchange);
		List<String> availableRefrenceIDList = new ArrayList<String>();
		if (responseArray != null) {
			for (int i = 0; i < responseArray.length(); i++) {
				JSONObject statusObj = responseArray.getJSONObject(i);
				if (statusObj.has("seller_sku")) {
					availableRefrenceIDList.add(statusObj.getString("seller_sku"));
				} else {
					availableRefrenceIDList.add(statusObj.getString("SellerSKU"));
				}
				status.add(statusObj);
			}
			if (skuList.size() != responseArray.length()) {
				skuList.removeAll(availableRefrenceIDList);
				exchange.setProperty("missedSKUinQCPolling", skuList);
			}
		}
		exchange.getOut().setBody(status);
	}

	private JSONArray callApi(HashMap<String, String> params, String scApiHost, String accessToken, List<String> skuList,
			Exchange exchange) throws JSONException {
		String response = "";
		try {
			String queryParam = "&limit=0&offset=0&seller_skus=" + URLEncoder.encode(skuList.toString(), "UTF-8");
			String clientID = Config.getConfig().getLazadaClientID();
			String clientSecret = Config.getConfig().getLazadaClientSecret();
			response = NewLazadaConnectionUtil.callAPI(scApiHost, "/product/qc/status/get", accessToken, params, "",
					queryParam, "GET", clientID, clientSecret);
		} catch (Exception e) {
			log.error("Exception occurred during Qc status for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " with nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response: " + response);
			e.printStackTrace();
		}
		JSONObject responseFromLazada = new JSONObject(response);
		if (responseFromLazada.has("code") && responseFromLazada.getString("code").equals("0")) {
			return responseFromLazada.getJSONArray("data");
		} else if (responseFromLazada.has("message")
				&& responseFromLazada.getString("message").toLowerCase().contains("sellersku not exist")) {
			exchange.setProperty("missedSKUinQCPolling", skuList);
			return null;
		}
		log.error("Error occured in checking QC Status for " + exchange.getProperty("accountNumber") + "-"
				+ exchange.getProperty("nickNameID") + " with ref. IDs" + skuList + " and response : " + response);
		return null;
	}

}