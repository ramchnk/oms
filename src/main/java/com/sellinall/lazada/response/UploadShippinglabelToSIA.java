package com.sellinall.lazada.response;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;

public class UploadShippinglabelToSIA implements Processor {
	static Logger log = Logger.getLogger(UploadShippinglabelToSIA.class.getName());

	public void process(Exchange exchange) throws Exception {
		String orderID = "";
		JSONObject response = null;
		try {
			JSONObject inBody = (JSONObject) exchange.getIn().getBody();
			orderID = exchange.getProperty("orderID", String.class);
			if (inBody.has("documents")) {
				JSONArray documentList = inBody.getJSONArray("documents");
				if (documentList.length() > 0) {
					JSONObject document = documentList.getJSONObject(0);
					if (document.has("file")) {
						String merchantID = exchange.getProperty("merchantID", String.class);
						String countryCode = exchange.getProperty("countryCode", String.class);
						JSONObject request = new JSONObject();
						request.put("merchantID", merchantID);
						request.put("type", "base64");
						request.put("orderID", orderID);
						request.put("countryCode", countryCode);
						request.put("nicknameID", exchange.getProperty("nickNameID",String.class));
						request.put("url", document.getString("file"));
						if (exchange.getProperties().containsKey("isEncodeShippingLabel")) {
							request.put("isEncodeShippingLabel",
									exchange.getProperty("isEncodeShippingLabel", Boolean.class));
						}
						Map<String, String> config = new HashMap<String, String>();
						config.put("Content-Type", "application/json");
						config.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
						response = HttpsURLConnectionUtil
								.doPut(Config.getConfig().getUploadShippinglabelToSELLinALLUrl(), request.toString(), config);
						if (response.getInt("httpCode") != 200 || !response.has("payload")) {
							log.error("Upload shipping label failed for orderID:" + orderID + " ,Response:" + response);
							return;
						}
						
						JSONObject uploadResponse = new JSONObject(response.getString("payload"));
						JSONArray responseList = uploadResponse.getJSONArray("response");
						JSONObject order = (JSONObject) exchange.getProperty("order");
						JSONObject documents = new JSONObject();
						documents.put("shippingLabelUrl", responseList.getJSONObject(0).getString("outputURL"));
						if (responseList.getJSONObject(0).has("encodedOutputURL")) {
							documents.put("encodedShippingLabelUrl",
									responseList.getJSONObject(0).getString("encodedOutputURL"));
						}
						order.put("documents", documents);
					}
				}
			}
		} catch (Exception e) {
			log.error("Upload shippingLabel failed for orderID: " + orderID + "and accountNumber: "
					+ exchange.getProperty("accountNumber") + " and nickNameID: "
					+ exchange.getProperty("nickNameID", String.class) + " and response: " + response.toString());
			e.printStackTrace();
		}

	}
}
