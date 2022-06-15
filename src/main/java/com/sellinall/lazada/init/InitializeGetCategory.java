package com.sellinall.lazada.init;

import java.net.URLDecoder;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;

public class InitializeGetCategory implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.getOut().setBody(inBody);
		String accountNumber = inBody.getString("accountNumber");
		String nickNameID = inBody.getString("nickNameID");
		exchange.setProperty("accountNumber", accountNumber);
		exchange.setProperty("nickNameID", nickNameID);
		exchange.setProperty("channelName", nickNameID.split("-")[0]);
		if (inBody.has("categoryID")) {
			exchange.setProperty("categoryID", inBody.getString("categoryID"));
		}
		if (inBody.has("itemTitle")) {
			exchange.setProperty("itemTitle", URLDecoder.decode(inBody.getString("itemTitle")));
		}
	}

}