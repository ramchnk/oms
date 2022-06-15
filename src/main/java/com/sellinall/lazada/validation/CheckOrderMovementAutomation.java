package com.sellinall.lazada.validation;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class CheckOrderMovementAutomation implements Processor {
	static Logger log = Logger.getLogger(CheckOrderMovementAutomation.class.getName());

	public void process(Exchange exchange) throws Exception {
		DBObject userDetails = (DBObject) exchange.getProperty("UserDetails");
		BasicDBObject channel = (BasicDBObject) userDetails.get("lazada");
		exchange.setProperty("isAutomationEnabled", false);
		if (channel.containsField("orderAutomation")) {
			BasicDBObject orderAutomation = (BasicDBObject) channel.get("orderAutomation");
			if (orderAutomation.containsField("enabled") && orderAutomation.getBoolean("enabled")) {
				String notificationStatus = exchange.getProperty("notificationStatus", String.class);
				if (notificationStatus.equalsIgnoreCase(orderAutomation.getString("fromState"))) {
					exchange.setProperty("isAutomationEnabled", true);
					String shippingProviderType = orderAutomation.getString("shippingProviderType");
					exchange.setProperty("shippingProviderType", shippingProviderType);
					exchange.setProperty("toState", orderAutomation.getString("toState"));
				}
			}
		}
	}
}
