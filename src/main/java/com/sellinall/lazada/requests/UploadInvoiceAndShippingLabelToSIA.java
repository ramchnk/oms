package com.sellinall.lazada.requests;

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

public class UploadInvoiceAndShippingLabelToSIA implements Processor {
	static Logger log = Logger.getLogger(UploadInvoiceAndShippingLabelToSIA.class.getName());

	public void process(Exchange exchange) throws Exception {
		String orderID = exchange.getProperty("orderID", String.class);
		// set document details
		JSONObject order = exchange.getProperty("order", JSONObject.class);
		JSONObject documents = new JSONObject();
		if (order.has("documents")) {
			documents = order.getJSONObject("documents");
		}
		JSONObject request = new JSONObject();
		String merchantID = exchange.getProperty("merchantID", String.class);
		String countryCode = exchange.getProperty("countryCode", String.class);
		request.put("merchantID", merchantID);
		request.put("type", "pdf");
		request.put("orderID", orderID);
		request.put("countryCode", countryCode);
		request.put("nicknameID", exchange.getProperty("nickNameID", String.class));
		if (exchange.getProperties().containsKey("isEncodeShippingLabel")) {
			request.put("isEncodeShippingLabel", exchange.getProperty("isEncodeShippingLabel", Boolean.class));
		}
		Map<String, String> config = new HashMap<String, String>();
		config.put("Content-Type", "application/json");
		config.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		// upload shipping label
		if (exchange.getProperties().containsKey("shippingLabelUrl")
				&& !(exchange.getProperty("shippingLabelUrl", String.class)).isEmpty()) {
			try {
				request.put("invoiceType", "shippingLabel");
				request.put("url", exchange.getProperty("shippingLabelUrl", String.class));
				JSONObject response = new JSONObject();
				for (int i = 0; i < 3; i++) {
					if ((response.has("httpCode") && response.getInt("httpCode") != 200) || !response.has("payload")) {
						response = HttpsURLConnectionUtil.doPut(Config.getConfig().getUploadShippinglabelToSELLinALLUrl(),
								request.toString(), config);
						Thread.sleep(2000);
					} else {
						break;
					}
				}
				if (response.getInt("httpCode") != 200 || !response.has("payload")) {
					log.error("Upload shipping label failed for orderID:" + orderID + " ,Response:" + response);
				} else {
					JSONObject uploadResponse = new JSONObject(response.getString("payload"));
					JSONArray responseList = uploadResponse.getJSONArray("response");
					documents.put("shippingLabelUrl", responseList.getJSONObject(0).getString("outputURL"));
					if (responseList.getJSONObject(0).has("encodedOutputURL")) {
						documents.put("encodedShippingLabelUrl", responseList.getJSONObject(0).getString("encodedOutputURL"));
					}
				}
			} catch (Exception e) {
				log.error("Upload shippingLabel failed for orderID: " + orderID + " and nickNameID: "
						+ exchange.getProperty("nickNameID", String.class) + " and merchantID " + merchantID);
				e.printStackTrace();
			}
		}
		// upload invoice
		if (exchange.getProperties().containsKey("invoiceUrl")
				&& !(exchange.getProperty("invoiceUrl", String.class)).isEmpty()) {
			try {
				request.put("invoiceType", "invoice");
				request.put("url", exchange.getProperty("invoiceUrl", String.class));
				JSONObject response = new JSONObject();
				for (int i = 0; i < 3; i++) {
					if ((response.has("httpCode") && response.getInt("httpCode") != 200) || !response.has("payload")) {
						response = HttpsURLConnectionUtil.doPut(Config.getConfig().getUploadShippinglabelToSELLinALLUrl(),
								request.toString(), config);
						Thread.sleep(2000);
					} else {
						break;
					}
				}
				if (response.getInt("httpCode") != 200 || !response.has("payload")) {
					log.error("Upload invoice failed for orderID:" + orderID + " ,Response:" + response);
				} else {
					JSONObject uploadResponse = new JSONObject(response.getString("payload"));
					JSONArray responseList = uploadResponse.getJSONArray("response");
					documents.put("invoiceUrl", responseList.getJSONObject(0).getString("outputURL"));
				}
			} catch (Exception e) {
				log.error("Upload invoice failed for orderID: " + orderID + " and nickNameID: "
						+ exchange.getProperty("nickNameID", String.class) + " and merchantID " + merchantID);
				e.printStackTrace();
			}
		}
		if (documents.length() > 0) {
			order.put("documents", documents);
		}
	}
}
