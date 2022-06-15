package com.sellinall.lazada.requests;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.sellinall.lazada.util.LazadaUtil;

public class RefreshSOFToken implements Processor {
	static Logger log = Logger.getLogger(RefreshSOFToken.class.getName());

	public void process(Exchange exchange) throws Exception {

		String userName = exchange.getProperty("sofUsername", String.class);
		String password = exchange.getProperty("sofPassword", String.class);
		String token = "";
		if (exchange.getProperties().containsKey("countryCode")) {
			token = LazadaUtil.getSOFToken(userName, password, exchange.getProperty("countryCode", String.class));
		}
		if (!token.isEmpty()) {
			log.info("Successfully created Seller own fleet token.");
			exchange.setProperty("sofToken", token);
		} else {
			log.error("Failed to refresh access token for this account number"
					+ exchange.getProperty("accountNumber", String.class) + ", & nicknameid : "
					+ exchange.getProperty("nickNameID", String.class));
		}
	}

}
