package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class InitGetFlexiCombo implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("accountNumber", inBody.getString("accountNumber"));
		exchange.setProperty("nickNameID", inBody.getString("nickNameID"));
		exchange.setProperty("isPromotionImportProcessing", true);
		exchange.setProperty("totalNumberOfPromotions", 0);
		exchange.setProperty("currentPage", 1);
		exchange.setProperty("pageSize", 50);
		exchange.setProperty("hasMoreRecordList", true);
	}
}
