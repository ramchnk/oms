package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class InitUpdateVoucherStatus implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject inbody = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("accountNumber", inbody.getString("accountNumber"));
		exchange.setProperty("nickNameID", inbody.getString("nickNameID"));
		exchange.setProperty("promotionID", inbody.getString("promotionID"));
		if (inbody.has("voucherType")) {
			exchange.setProperty("voucherType", inbody.getString("voucherType"));
		}
		exchange.setProperty("status", inbody.getString("status"));
	}

}
