package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;

public class ProcessUpdateByPage implements Processor {
	static Logger log = Logger.getLogger(ProcessUpdateByPage.class.getName());
	private static final int limit = 20;

	public void process(Exchange exchange) throws Exception {
		boolean isLastLoop = true;
		int index = exchange.getProperty("CamelLoopIndex", Integer.class);
		List<BasicDBObject> inventoryVariantDetails = new ArrayList<BasicDBObject>();
		if (exchange.getProperties().containsKey("inventoryDetails")) {
			ArrayList<BasicDBObject> inventoryDetails = (ArrayList<BasicDBObject>) exchange.getProperty("inventoryDetails");
			if (inventoryDetails.size() > 0) {
				int noOfPage = (inventoryDetails.size() / limit) + (inventoryDetails.size() % limit > 0 ? 1 : 0);
				int fromIndex = index * limit;
				int toIndex = fromIndex + limit;
				if (noOfPage == (index + 1)) {
					toIndex = inventoryDetails.size();
				} else {
					isLastLoop = false;
				}
				inventoryVariantDetails = inventoryDetails.subList(fromIndex, toIndex);
				exchange.setProperty("inventoryVariantDetails", inventoryVariantDetails);
				// if the variant has more than 20 child
				BasicDBObject inventory = exchange.getProperty("inventory", BasicDBObject.class);
				exchange.getOut().setBody(inventory);
			}
		}
		exchange.setProperty("isLastLoop", isLastLoop);
	}
}
