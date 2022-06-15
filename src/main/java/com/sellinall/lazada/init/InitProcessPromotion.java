package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class InitProcessPromotion implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.getOut().setBody(inBody);
		String action = "", promotionType = "";
		if (inBody.has("action")) {
			action = inBody.getString("action");
		}
		if (inBody.has("promotionType")) {
			promotionType = inBody.getString("promotionType");
		}
		exchange.setProperty("action", action);
		exchange.setProperty("promotionType", promotionType);
	}

}
