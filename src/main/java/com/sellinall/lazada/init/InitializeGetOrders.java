package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;

public class InitializeGetOrders implements Processor {
	static Logger log = Logger.getLogger(InitializeGetOrders.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject request = exchange.getIn().getBody(JSONObject.class);
		log.debug("InBody: " + request);
		if (request.has("isStatusMismatch")) {
			exchange.setProperty("isStatusMismatch", request.getBoolean("isStatusMismatch"));
		}
		exchange.setProperty("accountNumber", request.getString("accountNumber"));
		exchange.setProperty("nickNameID", request.getString("nickNameID"));
		exchange.setProperty("fromDate", request.getString("fromDate"));
		exchange.setProperty("toDate", request.getString("toDate"));
		exchange.setProperty("pageNumber", request.getInt("pageNumber"));
		int pageOffSet = Integer.parseInt(Config.getConfig().getOrderLimit()) * (request.getInt("pageNumber") - 1);
		exchange.setProperty("pageOffSet", pageOffSet);
		exchange.setProperty("requestType", "checkMissingOrders");
		long lastRequestEndTime = System.currentTimeMillis() / 1000L;
		exchange.setProperty("lastRequestEndTime", lastRequestEndTime);
	}

}
