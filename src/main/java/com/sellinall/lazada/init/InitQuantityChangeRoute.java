package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;

public class InitQuantityChangeRoute implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		String accountNumber = inBody.getString("accountNumber");
		exchange.setProperty("accountNumber", accountNumber);
		exchange.setProperty("nickNameID", inBody.getString("nickNameID"));
		if (inBody.has("eventID")) {
			exchange.setProperty("eventID", inBody.getString("eventID"));
		}
		String testingAccountNumber = Config.getConfig().getTestAccountNumber();
		boolean isEligibleToUpdateFeeds = false;
		if(accountNumber.equals(testingAccountNumber)) {
			isEligibleToUpdateFeeds = true;
		}
		exchange.setProperty("isEligibleToUpdateFeeds", isEligibleToUpdateFeeds);
		exchange.setProperty("request", inBody);
	}

}
