package com.sellinall.lazada.requests;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;
import com.sellinall.util.enums.PromotionType;

public class UpdatePromotionStatusInDB implements Processor {

	static Logger log = Logger.getLogger(UpdatePromotionStatusInDB.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject payload = new JSONObject();
		payload.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		payload.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		payload.put("promotionID", exchange.getProperty("promotionID", String.class));
		payload.put("promotionType", PromotionType.VOUCHER.toString());
		payload.put("status", exchange.getProperty("status", String.class));

		if (exchange.getProperties().containsKey("failureReason")) {
			payload.put("updateStatus", "FAILED");
			payload.put("failureReason", exchange.getProperty("failureReason", String.class));
			payload.put("isVoucherStatusUpdated", false);
		} else {
			payload.put("updateStatus", "COMPLETED");
			payload.put("isVoucherStatusUpdated", true);
		}
		updateVoucherStatus(payload);
	}

	private void updateVoucherStatus(JSONObject payload) throws JSONException {
		String url = Config.getConfig().getSIAPromotionServerURL() + "/promotions/" + payload.getString("promotionID")
				+ "/status";
		Map<String, String> header = new HashMap<String, String>();
		header.put("Content-Type", "application/json");
		header.put("accountNumber", payload.getString("accountNumber"));
		header.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());

		JSONObject response = new JSONObject();
		try {
			response = HttpsURLConnectionUtil.doPut(url, payload.toString(), header);
			JSONObject responsePayload = new JSONObject(response.getString("payload"));
			if (response.getInt("httpCode") == HttpStatus.SC_OK && responsePayload.has("response")
					&& responsePayload.getString("response").equals("success")) {
				log.info("Voucher status updated for accountNumber : " + payload.getString("accountNumber")
						+ ", nickNameID : " + payload.getString("nickNameID") + ", promotionID : "
						+ payload.getString("promotionID"));
			} else {
				log.error("Failed to update voucher status for accountNumber : " + payload.getString("accountNumber")
						+ ", nickNameID : " + payload.getString("nickNameID") + ", promotionID : "
						+ payload.getString("promotionID") + " and response is" + response.toString());
			}
		} catch (Exception e) {
			log.error("Failed to update voucher status for accountNumber : " + payload.getString("accountNumber")
					+ ", nickNameID : " + payload.getString("nickNameID") + ", promotionID : "
					+ payload.getString("promotionID") + " and response is" + response.toString());
			e.printStackTrace();
		}
	}

}
