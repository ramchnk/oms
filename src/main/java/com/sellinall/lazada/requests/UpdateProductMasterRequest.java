package com.sellinall.lazada.requests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;

public class UpdateProductMasterRequest implements Processor {
	static Logger log = Logger.getLogger(UpdateProductMasterRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONArray customSKUListToPushProductMaster = new JSONArray();
		JSONArray customSKUListToPullProductMaster = new JSONArray();
		exchange.setProperty("isEligibleToUpdateStock", false);
		if (exchange.getProperties().containsKey("customSKUListToPushProductMaster")) {
			customSKUListToPushProductMaster = exchange.getProperty("customSKUListToPushProductMaster",
					JSONArray.class);
			exchange.setProperty("customSKUList", customSKUListToPushProductMaster);
			exchange.setProperty("isEligibleToUpdateStock", true);
		}
		if (exchange.getProperties().containsKey("customSKUListToPullProductMaster")) {
			customSKUListToPullProductMaster = exchange.getProperty("customSKUListToPullProductMaster",
					JSONArray.class);
		}
		Map<String, String> header = new HashMap<String, String>();
		header.put("Content-Type", "application/json");
		header.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		header.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		String url = exchange.getProperty("inventoryUrl", String.class) + "/productMaster/nickname";
		if (customSKUListToPushProductMaster.length() > 0) {
			submitPushPullNicknameListToProductMaster(url, customSKUListToPushProductMaster, header,
					exchange.getProperty("nickNameID", String.class), "push");
		}
		if (customSKUListToPullProductMaster.length() > 0) {
			submitPushPullNicknameListToProductMaster(url, customSKUListToPullProductMaster, header,
					exchange.getProperty("nickNameID", String.class), "pull");
		}
	}

	private void submitPushPullNicknameListToProductMaster(String url, JSONArray sellerSKUList,
			Map<String, String> header, String nickNameID, String type) throws JSONException {
		JSONObject payload = new JSONObject();
		payload.put("nickNameID", nickNameID);
		payload.put("sellerSKUList", sellerSKUList);
		JSONObject response = new JSONObject();
		try {
			if (type.equals("push")) {
				response = HttpsURLConnectionUtil.doPut(url, payload.toString(), header);
			} else {
				response = HttpsURLConnectionUtil.doDeleteWithBody(url, payload.toString(), header);
			}
			if (response.getInt("httpCode") == HttpStatus.SC_OK) {
				log.info("productMaster updated");
			} else {
				log.error("productMaster update failed and response: " + response.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
