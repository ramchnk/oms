/**
 * 
 */
package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.EncryptionUtil;

/**
 * @author vikraman
 * 
 */
public class InitializeAccountRoute implements Processor {
	static Logger log = Logger.getLogger(InitializeAccountRoute.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);

		if (!inBody.has("invoiceTemplate")) {
			inBody.put("invoiceTemplate", "1");
		}
		if (!inBody.has("descriptionTemplate")) {
			inBody.put("descriptionTemplate", "none");
		}
		if (inBody.has("nickNameID")) {
			exchange.setProperty("nickNameID", inBody.getString("nickNameID"));
		}
		if (inBody.has("countryCode")) {
			exchange.setProperty("syncCountryCode", inBody.getString("countryCode"));
		}
		if (inBody.has("isGlobalAccount")) {
			exchange.setProperty("isGlobalAccount", inBody.getBoolean("isGlobalAccount"));
		}
		if (inBody.has("authToken")) {
			exchange.setProperty("authToken", inBody.getString("authToken"));
		}
		if (inBody.has("isUpdatePolicies")) {
			exchange.setProperty("isUpdatePolicies", inBody.getBoolean("isUpdatePolicies"));
		}
		exchange.setProperty("getSOFToken", false);
		boolean isChatAuthRequest = false;
		if (inBody.has("appType")) {
			String appType = inBody.getString("appType");
			exchange.setProperty("appType", appType);
			if (appType.equals("chat")) {
				isChatAuthRequest = true;
			}
		}
		exchange.setProperty("isChatAuthRequest", isChatAuthRequest);
		if (inBody.has("sellerOwnFleet")) {
			JSONObject sellerOwnFleet = inBody.getJSONObject("sellerOwnFleet");
			if (sellerOwnFleet.has("username") && !sellerOwnFleet.getString("username").isEmpty()
					&& sellerOwnFleet.has("password") && !sellerOwnFleet.getString("password").isEmpty()) {
				exchange.setProperty("getSOFToken", true);
			}
		}
		if (inBody.has("currencyCode")) {
			exchange.setProperty("countryCode", LazadaUtil.currencyToCountryCodeMap.get(inBody.getString("currencyCode")));
		}
		log.debug("InBody: " + inBody);
		exchange.getOut().setBody(inBody);
		exchange.setProperty("accountNumber", inBody.get("accountNumber"));
		exchange.setProperty("request", inBody);
	}
}