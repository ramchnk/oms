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
import com.mongodb.DBCollection;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.util.enums.SIAInventoryStatus;

/**
 * @author vikraman
 * 
 */
public class LoadInventoryCount implements Processor {
	static Logger log = Logger.getLogger(LoadInventoryCount.class.getName());

	public void process(Exchange exchange) throws Exception {
		long outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private long createBody(Exchange exchange) {
		BasicDBObject countCondition = new BasicDBObject("accountNumber", exchange.getProperty("accountNumber"));
		long count;
		DBCollection inventory = DbUtilities.getROInventoryDBCollection("inventory");
		if (exchange.getProperties().containsKey("requestType")
				&& exchange.getProperty("requestType").equals("pullInventory")) {
			List<SIAInventoryStatus> exclusions = new ArrayList<SIAInventoryStatus>();
			exclusions.add(SIAInventoryStatus.REMOVED);
			List<String> statusValues = getStatusValues(exclusions);
			countCondition.put("status", new BasicDBObject("$in", statusValues));
			count = inventory.find(countCondition).hint("accountNumber_1_SKU_-1_status_1").count();
		} else {
			count = inventory.find(countCondition).count();
		}
		log.debug("inv count for account " + exchange.getProperty("accountNumber") + " is " + count);
		return count;
	}

	private static List<String> getStatusValues(List<SIAInventoryStatus> exclusions) {
		SIAInventoryStatus[] a = SIAInventoryStatus.values();
		List<String> statusValues = new ArrayList<String>();
		for (int i = 0; i < a.length; i++) {
			if (!exclusions.contains(a[i])) {
				statusValues.add(a[i].toString());
			}
		}
		return statusValues;
	}
}
