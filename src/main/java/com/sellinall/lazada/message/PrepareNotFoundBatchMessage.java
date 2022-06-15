package com.sellinall.lazada.message;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.lazada.util.LazadaUtil;

public class PrepareNotFoundBatchMessage implements Processor {
	static Logger log = Logger.getLogger(PrepareNotFoundBatchMessage.class.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		JSONObject SKUMap = exchange.getProperty("SKUMap", JSONObject.class);
		if (SKUMap == null) {
			ArrayList<BasicDBObject> feedMessages = exchange.getProperty("feedMessages", ArrayList.class);
			if (exchange.getIn().getHeaders().containsKey("isCustomSKUNotFoundMessage")) {
				feedMessages = exchange.getProperty("customSKUNotMessages", ArrayList.class);
			}
			BasicDBObject feedMessage = feedMessages.get(exchange.getProperty(Exchange.LOOP_INDEX, Integer.class));
			SKUMap = LazadaUtil.parseToJsonObject((DBObject) feedMessage);
			if (exchange.getProperties().containsKey("Action")
					&& exchange.getProperty("Action", String.class).equals("ProductStatusUpdate")) {
				exchange.setProperty("sellerSKU", SKUMap.getString("customSKU"));
				SKUMap.put("sellerSKU", SKUMap.getString("customSKU"));
			} else {
				exchange.setProperty("SKU", SKUMap.getString("SKU"));
			}
		}
		String failureReason = "";
		String warningMessage = "";

		if (exchange.getProperties().containsKey("failureReason")
				&& !exchange.getProperty("failureReason", String.class).isEmpty()) {
			failureReason = exchange.getProperty("failureReason", String.class);
		}
		if (exchange.getProperties().containsKey("warningMessage")) {
			warningMessage = exchange.getProperty("warningMessage", String.class);
		}
		if (exchange.getIn().getHeaders().containsKey("isCustomSKUNotFoundMessage")) {
			failureReason = "Seller SKU not found";
		}
		if (!failureReason.isEmpty()) {
			SKUMap.put("status", "failure");
			if (!warningMessage.isEmpty()) {
				failureReason = failureReason + "\n" + warningMessage;
			}
			SKUMap.put("failureReason", failureReason);
		} else {
			// for QC in lazada variant children and post as Non-variant
			String status = "success";
			SKUMap.put("status", status);
			if (!warningMessage.isEmpty()) {
				SKUMap.put("warningMessage", warningMessage);
			}
		}

		log.debug("SKUMap :" + SKUMap);
		exchange.getOut().setBody(SKUMap);
	}

}
