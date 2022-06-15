package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAInventoryUpdateStatus;

public class CheckIsEligibleToUpdateParentStatus implements Processor {

	public void process(Exchange exchange) throws Exception {

		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String SKU = exchange.getProperty("parentSKU", String.class);
		List<String> statuses = new ArrayList<String>();
		statuses.add(SIAInventoryStatus.ACTIVE.toString());
		statuses.add(SIAInventoryStatus.SOLDOUT.toString());
		DBObject query = new BasicDBObject();
		query.put("accountNumber", accountNumber);
		query.put("SKU", new BasicDBObject("$regex", "^" + SKU + "-*"));
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("nickNameID", nickNameID);
		elemMatch.put("status", new BasicDBObject("$in", statuses));
		elemMatch.put("variantDetails", new BasicDBObject("$exists", true));
		query.put("lazada", new BasicDBObject("$elemMatch", elemMatch));

		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		boolean isActiveChildFound = table.count(query) > 0 ? true : false;
		String currentStatus = SIAInventoryStatus.ACTIVE.toString();
		if (!isActiveChildFound) {
			currentStatus = SIAInventoryStatus.INACTIVE.toString();
		}

		DBObject dbquery = new BasicDBObject();
		dbquery.put("accountNumber", accountNumber);
		dbquery.put("SKU", SKU);
		dbquery.put("lazada.nickNameID", nickNameID);
		DBObject updateInventory = new BasicDBObject();
		updateInventory.put("lazada.$.status", currentStatus);
		updateInventory.put("lazada.$.updateStatus", SIAInventoryUpdateStatus.COMPLETED.toString());
		updateInventory.put("lazada.$.timeLastUpdated", System.currentTimeMillis() / 1000L);
		DBObject updateObject = new BasicDBObject("$set", updateInventory);
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
		exchange.getOut().setBody(new Object[] { dbquery, updateObject });
		exchange.setProperty("isEligibleToUpdateParentStatus", true);
	}

}
