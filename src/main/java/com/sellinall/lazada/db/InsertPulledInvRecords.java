/**
 * 
 */
package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.sellinall.util.InventorySequence;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAUnlinkedInventoryStatus;

/**
 * @author Senthil
 * 
 */
public class InsertPulledInvRecords implements Processor {
	static Logger log = Logger.getLogger(InsertPulledInvRecords.class.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {

		List<BasicDBObject> unlinkedInventoryDocs = (List<BasicDBObject>) exchange.getProperty("unlinkedInventoryDocs");
		List<BasicDBObject> inventoryDocs = new ArrayList<BasicDBObject>();
		for (BasicDBObject unlinkedInventoryDoc : unlinkedInventoryDocs) {
			BasicDBObject inventoryDoc = new BasicDBObject(unlinkedInventoryDoc);
			String merchantID = inventoryDoc.getString("merchantId");
			String SKU = InventorySequence.getNextSKU(merchantID);
			inventoryDoc.put("status", SIAInventoryStatus.INITIATED);
			inventoryDoc.put("SKU", SKU);
			inventoryDocs.add(inventoryDoc);
			unlinkedInventoryDoc.put("linkedSKU", SKU);
			unlinkedInventoryDoc.put("status", SIAUnlinkedInventoryStatus.LINKED.toString());
		}

		exchange.getOut().setBody(inventoryDocs);
	}
}