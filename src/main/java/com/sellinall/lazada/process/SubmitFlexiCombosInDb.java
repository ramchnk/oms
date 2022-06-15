package com.sellinall.lazada.process;

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

public class SubmitFlexiCombosInDb implements Processor {
	static Logger log = Logger.getLogger(SubmitFlexiCombosInDb.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject flexiComboItem = exchange.getProperty("flexiComboForDB", JSONObject.class);
		String url = Config.getConfig().getSIAPromotionServerURL() +"/promotions" ;
		Map<String, String> header = new HashMap<String, String>();
		header.put("Content-Type", "application/json");
		header.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		header.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		submitFlexiComboDBUpdate(url, header, flexiComboItem, exchange);
	}

	private void submitFlexiComboDBUpdate(String url, Map<String, String> header, JSONObject promotionData,
			Exchange exchange) throws JSONException {
		JSONObject payload = new JSONObject();
		payload.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		payload.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		payload.put("isImportedFlexiCombo", true);
		payload.put("promotionType", PromotionType.FLEXI_COMBO.toString());
		payload.put("promotionData", promotionData);
		JSONObject response = new JSONObject();
		try {
			response = HttpsURLConnectionUtil.doPost(url, payload.toString(), header);
			JSONObject responsePayload = new JSONObject(response.getString("payload"));
			if (response.getInt("httpCode") == HttpStatus.SC_OK && responsePayload.has("response")
					&& responsePayload.getString("response").equals("success")) {
				log.info("Imported Flexi Combo Data updated for accountNumber : "
						+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
						+ exchange.getProperty("nickNameID", String.class));
			} else {
				exchange.setProperty("failureReason", "Unable to import");
				log.error("Imported Flexi Combo Data update failed  for accountNumber : "
						+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
						+ exchange.getProperty("nickNameID", String.class) + " and response is" + response.toString());
			}
		} catch (Exception e) {
			exchange.setProperty("failureReason", "Unable to import");
			log.error("Imported Flexi Combo update failed  for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response is" + response.toString(), e);
		}
	}
}
