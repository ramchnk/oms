package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class InitProcessLazadaNotification implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		String notificationType = "";
		if(inBody.has("data")) {
			JSONObject data = inBody.getJSONObject("data");
			if (data.has("trade_order_id")) {
				notificationType = "order";
			} else if (data.has("session_id")) {
				notificationType = "chat";
				if(data.has("from_account_type")) {
					exchange.setProperty("fromAccountType", data.get("from_account_type"));
				}
				if(data.has("sync_type")) {
					exchange.setProperty("syncType", data.get("sync_type"));
				}
			}
		}
		if (inBody.has("site") && inBody.getString("site").contains("_")) {
			String countryCode = inBody.getString("site").split("_")[1];
			inBody.put("countryCode", countryCode.toUpperCase());
		}
		exchange.setProperty("sellerID", inBody.getString("seller_id"));
		exchange.setProperty("notificationType", notificationType);
		exchange.getOut().setBody(inBody);
	}
}
