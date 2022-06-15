package com.sellinall.lazada.requests;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.DateUtil;
import com.sellinall.util.EncryptionUtil;
import com.sellinall.util.HttpsURLConnectionUtil;
import com.sellinall.util.enums.OrderUpdateStatus;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.SIAShippingStatus;

public class SubmitDeliveredStatustoSOFP implements Processor {

	static Logger log = Logger.getLogger(SubmitDeliveredStatustoSOFP.class.getName());
	public void process(Exchange exchange) throws Exception {
		String sofToken = exchange.getProperty("sofToken", String.class);
		Map<String, String> config = new HashMap<String, String>();
		config.put("Content-Type", "application/json");
		config.put("Authorization", "JWT " + sofToken);
		String countryCode = exchange.getProperty("countryCode", String.class);
		String timeZone = LazadaUtil.timeZoneCountryMap.get(countryCode);
		String timeFormat = DateUtil.getDateFromSIAFormat(System.currentTimeMillis() / 1000L, "yyyy-MM-dd HH:mm:ss",
				timeZone);
		exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
		JSONObject payloads = new JSONObject();
		if (!Config.getConfig().getSofEnabledCountries().contains(countryCode)) {
			log.warn("Seller Own Fleet not implemented for this country : " + countryCode);
			return;
		}
		String trackingNumber = exchange.getProperty("sofReferenceNumber", String.class);
		payloads.put("tracking_number", trackingNumber);
		payloads.put("status", SIAOrderStatus.DELIVERED.toString().toLowerCase());
		payloads.put("timestamp", timeFormat);
		String url = Config.getConfig().getSOFPortalUrl(countryCode) + "statuses/";

		JSONObject response = HttpsURLConnectionUtil.doPost(url, payloads.toString(), config);
		if (response.getInt("httpCode") == HttpStatus.SC_OK) {
			log.info("order updated in seller own fleet portal : " + trackingNumber);
			exchange.setProperty("updateStatus", OrderUpdateStatus.COMPLETE.toString());
			// To mark as delivered in our system
			JSONObject order = (JSONObject) exchange.getProperty("order");
			order.put("orderStatus", SIAOrderStatus.DELIVERED.toString());
			order.put("shippingStatus", SIAShippingStatus.DELIVERED.toString());
		} else {
			log.error("seller own fleet portal update failed for tracking number: " + trackingNumber + " and response: "
					+ response.toString());
		}
	}

}
