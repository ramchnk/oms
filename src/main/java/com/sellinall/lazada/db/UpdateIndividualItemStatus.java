package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAInventoryUpdateStatus;

public class UpdateIndividualItemStatus implements Processor {

	static Logger log = Logger.getLogger(UpdateIndividualItemStatus.class.getName());

	public void process(Exchange exchange) throws Exception {
		Object[] outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private Object[] createBody(Exchange exchange) throws Exception {
		BasicDBObject updateInventory = new BasicDBObject();
		DBObject dbquery = new BasicDBObject();
		String currentStatus = "";
		String updateStatus = "";
		boolean isPostingSuccess = exchange.getProperty("isPostingSuccess", Boolean.class);
		String failureReason = "";
		String channelName = exchange.getProperty("channelName", String.class);
		BasicDBObject inventory = exchange.getProperty("inventory", BasicDBObject.class);
		String SKU = inventory.getString("SKU");
		if (exchange.getProperties().containsKey("isPartialAutoStatusUpdate")
				&& !exchange.getProperty("isPartialAutoStatusUpdate", Boolean.class)
				&& exchange.getProperties().containsKey("eligibleToUpdateAutoStatus")
				&& exchange.getProperty("eligibleToUpdateAutoStatus", Boolean.class)) {
			SKU = SKU.split("-")[0];
		}
		BasicDBObject channelInventory = (BasicDBObject) inventory.get("lazada");
		dbquery.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		dbquery.put("SKU", Pattern.compile("^" + SKU + ".*"));
		dbquery.put(channelName + ".nickNameID", channelInventory.getString("nickNameID"));
		if (exchange.getProperties().containsKey("requestType")
				&& exchange.getProperty("requestType", String.class).equals("updateItem")
				&& exchange.getProperties().containsKey("isStatusUpdate")
				&& exchange.getProperty("isStatusUpdate", Boolean.class)) {
			dbquery.put("lazada.status", new BasicDBObject("$in", Arrays.asList(SIAInventoryStatus.ACTIVE.toString(),
					SIAInventoryStatus.INACTIVE.toString(), SIAInventoryStatus.PENDING.toString())));
		} else {
			dbquery.put("lazada.status", new BasicDBObject("$ne", SIAInventoryStatus.FAILED.toString()));
		}
		currentStatus = channelInventory.getString("status");
		String requestType = "";
		if (exchange.getProperties().containsKey("requestType")) {
			requestType = exchange.getProperty("requestType", String.class);
		}
		if (requestType.equals("batchEditItem") || (!requestType.equals("updateItem")
				&& exchange.getProperties().containsKey("eligibleToUpdateAutoStatus")
				&& exchange.getProperty("eligibleToUpdateAutoStatus", Boolean.class))) {
			/*
			 * for batch status update, status won't update in batch server, so
			 * based on updateToStatus in request message we will update
			 * inventory status
			 */
			if (isPostingSuccess) {
				updateStatus = SIAInventoryUpdateStatus.COMPLETED.toString();
				if (exchange.getProperty("updateToStatus", String.class).equals("active")) {
					currentStatus = SIAInventoryStatus.ACTIVE.toString();
				} else if (exchange.getProperty("updateToStatus", String.class).equals("inactive")) {
					currentStatus = SIAInventoryStatus.INACTIVE.toString();
				}
				exchange.setProperty("isEligibleToUpdatePM", true);
			} else {
				updateStatus = SIAInventoryUpdateStatus.FAILED.toString();
				List<String> failureReasons = new ArrayList<String>();
				if (exchange.getProperties().containsKey("failureReasons")) {
					failureReasons = exchange.getProperty("failureReasons", List.class);
				}
				failureReasons.add(exchange.getProperty("SKU", String.class) + " : "
						+ exchange.getProperty("failureReason", String.class));
				exchange.setProperty("isEligibleToUpdatePM", false);
			}
		} else {
			if (isPostingSuccess) {
				updateStatus = SIAInventoryUpdateStatus.COMPLETED.toString();
			} else if (!isPostingSuccess && exchange.getProperties().containsKey("requestType")
					&& !exchange.getProperty("requestType", String.class).equals("quantityChange")) {
				/* Here we will revert back to old status for failure Cases. */
				if (currentStatus.equals(SIAInventoryStatus.ACTIVE.toString())) {
					currentStatus = SIAInventoryStatus.INACTIVE.toString();
				} else if (currentStatus.equals(SIAInventoryStatus.INACTIVE.toString())) {
					currentStatus = SIAInventoryStatus.ACTIVE.toString();
				}
				updateStatus = SIAInventoryUpdateStatus.FAILED.toString();
			}
		}
		if (exchange.getProperties().containsKey("failureReason")
				&& !exchange.getProperty("failureReason", String.class).isEmpty()) {
			failureReason = exchange.getProperty("failureReason", String.class);
		}
		updateInventory.put(channelName + ".$.updateStatus", updateStatus);
		updateInventory.put(channelName + ".$.status", currentStatus);
		updateInventory.put(channelName + ".$.failureReason", failureReason);
		updateInventory.put(channelName + ".$.timeLastUpdated", System.currentTimeMillis() / 1000L);
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
		exchange.getOut().setHeader(MongoDbConstants.MULTIUPDATE, true);
		DBObject updateObject = new BasicDBObject("$set", updateInventory);
		return new Object[] { dbquery, updateObject };
	}
}
