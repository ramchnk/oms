package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class InitProcessRemoveItem implements Processor {
	static Logger log = Logger.getLogger(InitProcessRemoveItem.class.getName());
	
	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		boolean isRequestFromBatch = false;
		if (inBody.has("SKUMap") && inBody.getString("requestType").equals("removeArrayItem")) {
			exchange.setProperty("SKUMap", inBody.getJSONArray("SKUMap").get(0));
			isRequestFromBatch = true;
		}
		exchange.setProperty("isRequestFromBatch", isRequestFromBatch);
	}

}