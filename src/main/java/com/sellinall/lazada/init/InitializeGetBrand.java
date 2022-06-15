package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class InitializeGetBrand implements Processor{

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.getOut().setBody(inBody);
		String accountNumber = inBody.getString("accountNumber");
		String nickNameID = inBody.getString("nickNameID");
		exchange.setProperty("accountNumber", accountNumber);
		exchange.setProperty("offset", inBody.getString("offset"));
		exchange.setProperty("limit", inBody.getString("pageLimit"));
		exchange.setProperty("nickNameID", nickNameID);
		exchange.setProperty("channelName", nickNameID.split("-")[0]);
	}
}
