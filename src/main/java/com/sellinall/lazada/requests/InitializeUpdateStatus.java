package com.sellinall.lazada.requests;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;

public class InitializeUpdateStatus implements Processor {
	static Logger log = Logger.getLogger(InitializeUpdateStatus.class.getName());

	public void process(Exchange exchange) throws Exception {
		exchange.setProperty("isPostingSuccess", false);
		exchange.setProperty("isStatusUpdate", true);
		BasicDBObject details = new BasicDBObject();
		details.put("nickNameID", exchange.getProperty("nickNameID"));
		details.put("status", exchange.getProperty("itemCurrentStatus"));
		BasicDBObject inventory = new BasicDBObject();
		inventory.put("SKU", exchange.getProperty("SKU"));
		inventory.put("lazada", details);
		// just inventory created for status update only, otherwise don't use this
		// inventory property
		exchange.setProperty("inventory", inventory);

		boolean eligibleToUpdateAutoStatus = exchange.getProperties().containsKey("eligibleToUpdateAutoStatus")
				? exchange.getProperty("eligibleToUpdateAutoStatus", Boolean.class)
				: false;
		String statusToUpdate = exchange.getProperty("statusToUpdate", String.class);
		if (!exchange.getProperties().containsKey("itemID") && eligibleToUpdateAutoStatus && statusToUpdate != null) {
			LazadaUtil.loadItemIDFromDB(exchange);
		}
	}

}