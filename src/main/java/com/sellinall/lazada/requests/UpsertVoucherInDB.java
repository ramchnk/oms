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

public class UpsertVoucherInDB implements Processor {
	static Logger log = Logger.getLogger(UpsertVoucherInDB.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject voucherDetails = exchange.getIn().getBody(JSONObject.class);
		String url = Config.getConfig().getSIAPromotionServerURL() + "/promotions";
		Map<String, String> header = new HashMap<String, String>();
		header.put("Content-Type", "application/json");
		header.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		header.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		submitVoucherDBUpdate(url, header, voucherDetails, exchange);
	}

	private void submitVoucherDBUpdate(String url, Map<String, String> header, JSONObject voucherData,
			Exchange exchange) throws JSONException {
		JSONObject payload = new JSONObject();
		payload.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		payload.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		payload.put("isImportedVoucher", true);
		payload.put("promotionType", PromotionType.VOUCHER.toString());
		payload.put("promotionData", voucherData);
		JSONObject response = new JSONObject();
		try {
			response = HttpsURLConnectionUtil.doPost(url, payload.toString(), header);
			JSONObject responsePayload = new JSONObject(response.getString("payload"));
			if (response.getInt("httpCode") == HttpStatus.SC_OK && responsePayload.has("response")
					&& responsePayload.getString("response").equals("success")) {
				log.info("Imported voucher Data updated for accountNumber : "
						+ exchange.getProperty("accountNumber", String.class) + ", nickNameID : "
						+ exchange.getProperty("nickNameID", String.class) + " and promotionID : "
						+ voucherData.getString("promotionID"));
			} else {
				log.error("Imported voucher Data update failed  for accountNumber : "
						+ exchange.getProperty("accountNumber", String.class) + ", nickNameID : "
						+ exchange.getProperty("nickNameID", String.class) + ", promotionID : "
						+ voucherData.getString("promotionID") + " and response is" + response.toString());
			}
		} catch (Exception e) {
			log.error("Imported voucher update failed  for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + ", nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + ", promotionID : "
					+ voucherData.getString("promotionID") + " and response is" + response.toString(), e);
		}
	}

}
