package com.sellinall.lazada.db;

import java.util.ArrayList;
import com.mongodb.WriteConcern;
import org.apache.camel.component.mongodb.MongoDbConstants;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAInventoryUpdateStatus;

public class CheckAndUpdateParentStatus implements Processor {

	public void process(Exchange exchange) throws Exception {
		boolean isPostingSuccess = exchange.getProperty("isPostingSuccess", Boolean.class);
		exchange.setProperty("isEligibleToUpdateParentStatus", false);
		if (!isPostingSuccess) {
			return;
		}
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		BasicDBObject inventory = exchange.getProperty("inventory", BasicDBObject.class);
		BasicDBObject channelInventory = (BasicDBObject) inventory.get("lazada");
		String SKU = inventory.getString("SKU");

		String currentStatus = channelInventory.getString("status");
		if ((exchange.getProperties().containsKey("requestType")
				&& exchange.getProperty("requestType", String.class).equals("batchEditItem"))
				|| (exchange.getProperties().containsKey("isAutoStatusUpdate")
				&& exchange.getProperty("isAutoStatusUpdate", Boolean.class))) {
			if (exchange.getProperty("updateToStatus", String.class).equals("active")) {
				currentStatus = SIAInventoryStatus.ACTIVE.toString();
			} else if (exchange.getProperty("updateToStatus", String.class).equals("inactive")) {
				currentStatus = SIAInventoryStatus.INACTIVE.toString();
			}
		}
		boolean needToUpdateParentStatus = true;
		if (currentStatus.equals(SIAInventoryStatus.INACTIVE.toString())) {
			needToUpdateParentStatus = checkForParentStatusUpdate(accountNumber,
					channelInventory.getString("nickNameID"), SKU.split("-")[0]);
		}
		if (!needToUpdateParentStatus) {
			return;
		}
		exchange.setProperty("isEligibleToUpdateParentStatus", true);
		DBObject dbquery = new BasicDBObject();
		dbquery.put("accountNumber", accountNumber);
		dbquery.put("SKU", SKU.split("-")[0]);
		dbquery.put("lazada.nickNameID", channelInventory.getString("nickNameID"));
		DBObject updateInventory = new BasicDBObject();
		updateInventory.put("lazada.$.status", currentStatus);
		updateInventory.put("lazada.$.updateStatus", SIAInventoryUpdateStatus.COMPLETED.toString());
		updateInventory.put("lazada.$.timeLastUpdated", System.currentTimeMillis() / 1000L);
		DBObject updateObject = new BasicDBObject("$set", updateInventory);
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
		exchange.getOut().setBody(new Object[] { dbquery, updateObject });
	}

	private boolean checkForParentStatusUpdate(String accountNumber, String nickNameID, String SKU) {
		List<String> statuses = new ArrayList<String>();
		statuses.add(SIAInventoryStatus.ACTIVE.toString());
		statuses.add(SIAInventoryStatus.SOLDOUT.toString());
		DBObject query = new BasicDBObject();
		query.put("accountNumber", accountNumber);
		query.put("lazada.nickNameID", nickNameID);
		query.put("lazada.status", new BasicDBObject("$in", statuses));
		query.put("lazada.variantDetails", new BasicDBObject("$exists", true));
		query.put("SKU", new BasicDBObject("$regex", "^" + SKU));

		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		return table.count(query) > 0 ? false : true;
	}
}
