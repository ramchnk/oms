package com.sellinall.lazada.db;


import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mongodb.BasicDBObject;
import com.sellinall.util.InventorySequence;

public class ProcessUnlinkedSearchResult implements Processor {

	public void process(Exchange exchange) throws Exception {
		BasicDBObject unlinkedInventory = exchange.getIn().getBody(BasicDBObject.class);
		if (unlinkedInventory == null) {
			String inventorySequence="";
			if (exchange.getProperty("isInventoryEmpty", Boolean.class)) {
				//We can add directly to linked inventory 
				inventorySequence = InventorySequence.getNextSKU(exchange.getProperty("merchantID", String.class));
			}else{
				inventorySequence = InventorySequence.getNextUnlinkedSKU(exchange.getProperty("merchantID", String.class));
			}
			exchange.setProperty("SKU", inventorySequence);
			exchange.setProperty("isNewItem", true);
			return;
		}
		exchange.setProperty("isNewItem", false);
		exchange.setProperty("SKU", unlinkedInventory.getString("SKU"));
	}

}