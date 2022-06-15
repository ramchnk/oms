package com.sellinall.lazada.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;

import net.sf.json.JSONArray;

public class ProcessImageUpdateResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessImageUpdateResponse.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		if (exchange.getProperties().containsKey("imageUpdateFailedsellerSKUMap")) {
			Map<String, String> imageUpdateFailedsellerSKUMap = (HashMap<String, String>) exchange
					.getProperty("imageUpdateFailedsellerSKUMap");
			Map<String, String> sellerSKUMap = (HashMap<String, String>) exchange.getProperty("sellerSKUMap");
			for (Entry<String, String> entry : imageUpdateFailedsellerSKUMap.entrySet()) {
				String sellerSKU = entry.getKey();
				String SKU = sellerSKUMap.get(sellerSKU);
				String failureReason = entry.getValue();
				DBObject dbquery = new BasicDBObject();
				dbquery.put("accountNumber", accountNumber);
				dbquery.put("SKU", SKU);
				dbquery.put("lazada.nickNameID", nickNameID);
				DBObject updateObj = new BasicDBObject();
				updateObj.put("lazada.$.failureReason", failureReason);
				DBObject updateData = new BasicDBObject();
				updateData.put("$set", updateObj);
				updateInventory(dbquery, updateData);
			}
		}
	}

	private void updateInventory(DBObject dbquery, DBObject updateData) {
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		table.update(dbquery, updateData);
	}

}
