package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class SubmitUpdateRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitUpdateRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		String response = null;
		String requestType = "";
		if(exchange.getProperty("requestType") != null) {
			requestType = exchange.getProperty("requestType", String.class);
		}
		String payload = exchange.getIn().getBody(String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		String url = exchange.getProperty("hostURL", String.class);
		map.put("access_token", accessToken);
		map.put("payload", payload);
		String requestBody = "payload=" + URLEncoder.encode(payload);
		log.debug("update item payLoad =" + payload);
		String apiName = "/product/price_quantity/update";
		try {
			if (exchange.getProperties().containsKey("isUpdateQuantityByDiff")
					&& exchange.getProperty("isUpdateQuantityByDiff", Boolean.class)) {
				apiName = "/product/stock/sellable/adjust";
			} else if ((requestType.equals("updateItem") || requestType.equals("quantityChange"))
					&& exchange.getProperties().containsKey("isUpdateSellableStock")
					&& exchange.getProperty("isUpdateSellableStock", Boolean.class)) {
				apiName = "/product/stock/sellable/update";
			} else if (requestType.equals("quantityChange")
					&& exchange.getProperties().containsKey("isUpdateStockViaProductUpdateApi")
					&& exchange.getProperty("isUpdateStockViaProductUpdateApi", Boolean.class)) {
				apiName = "/product/update";
			}
			String clientID = Config.getConfig().getLazadaClientID();
			String clientSecret = Config.getConfig().getLazadaClientSecret();
			response = NewLazadaConnectionUtil.callAPI(url, apiName, accessToken, map, requestBody, "", "POST",
					clientID, clientSecret);
			String sellerSKU = "";
			if (exchange.getProperty("refrenceID") != null) {
				sellerSKU = exchange.getProperty("refrenceID", String.class);
			}
			log.info("update item done for SKU:" + exchange.getProperty("SKU") + ", sellerSKU:" + sellerSKU
					+ " apiName : " + apiName + " , Update Item Response : " + response);
			JSONObject channelResponse = new JSONObject(response);
			exchange.setProperty("stockUpdateResponse", channelResponse);
			exchange.getOut().setBody(channelResponse);
		} catch (Exception e) {
			log.error("Exception occured while updating item for SKU: " + exchange.getProperty("SKU")
					+ " ,accountNumber : " + exchange.getProperty("accountNumber", String.class) + " nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " apiName : " + apiName + " and response : "
					+ response);
			e.printStackTrace();
			JSONObject responseMsg = new JSONObject();
			responseMsg.put("status", "failure");
			exchange.getOut().setBody(responseMsg);
		}
	}

}