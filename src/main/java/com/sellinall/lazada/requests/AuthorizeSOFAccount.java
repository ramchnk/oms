package com.sellinall.lazada.requests;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;

public class AuthorizeSOFAccount implements Processor {
	static Logger log = Logger.getLogger(AuthorizeSOFAccount.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		JSONObject sellerOwnFleet = inBody.getJSONObject("sellerOwnFleet");
		String userName = sellerOwnFleet.getString("username");
		String password = sellerOwnFleet.getString("password");
		String token = "";
		if (exchange.getProperties().containsKey("countryCode")) {
			token = LazadaUtil.getSOFToken(userName, password,
					exchange.getProperty("countryCode", String.class));
		}
		if (!token.isEmpty()) {
			log.info("Successfully created Seller own fleet token.");
			exchange.setProperty("sofToken", token);
		} else {
			exchange.setProperty("failureReason", "Seller Own Fleet Authentication failed.");
			log.error("Seller Own Fleet Authentication failed.");
		}
		exchange.getOut().setBody(inBody);
	}

}
