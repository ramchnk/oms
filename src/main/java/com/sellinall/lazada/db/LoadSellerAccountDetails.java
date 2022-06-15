package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.lazada.util.LazadaUtil;

public class LoadSellerAccountDetails implements Processor {

	static Logger log = Logger.getLogger(LoadSellerAccountDetails.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		String sellerID = exchange.getProperty("sellerID", String.class);
		exchange.getOut().setBody(inBody);
		exchange.setProperty("isEligibleToProceed", false);

		DBObject sellerIdQuery = new BasicDBObject();
		sellerIdQuery.put("lazada.postHelper.sellerID", sellerID);

		DBObject sellerIdsQuery = new BasicDBObject();
		sellerIdsQuery.put("lazada.postHelper.sellerIDList.sellerID", sellerID);

		List<DBObject> orQuery = new ArrayList<DBObject>();
		orQuery.add(sellerIdQuery);
		orQuery.add(sellerIdsQuery);

		DBObject query = new BasicDBObject();
		query.put("$or", orQuery);

		DBObject projection = new BasicDBObject();
		projection.put("merchantID", 1);
		projection.put("isEligibleToPullMissingItem", 1);
		projection.put("individualSKUPerChannel", 1);
		projection.put("lazada", 1);

		DBCollection table = DbUtilities.getDBCollection("accounts");
		List<DBObject> results = table.find(query, projection).toArray();

		if (results.isEmpty()) {
			// Note: will enable this line once sellerID added for all accounts
			log.error("can't able to find seller details for the notification : " + inBody);
			return;
		}
		DBObject result = results.get(0);
		List<DBObject> lazadaObjList = (List<DBObject>) result.get("lazada");
		boolean isEligibleToProceed = true;
		DBObject lazadaObj = LazadaUtil.getChannelObjBySellerID(lazadaObjList, sellerID);
		if (lazadaObj.containsField("status") && lazadaObj.get("status").toString().equals("X")) {
			isEligibleToProceed = false;
		}
		if (lazadaObj.containsField("processOrdersWithSKUOnly")
				&& (Boolean) lazadaObj.get("processOrdersWithSKUOnly")) {
			getCommonAccountDetails(exchange, results, sellerID);
			if (results.size() > 0) {
				isEligibleToProceed = true;
			}
		}
		result.put("lazada", lazadaObj);
		exchange.setProperty("isEligibleToProceed", isEligibleToProceed);
		exchange.setProperty("UserDetails", result);
		exchange.setProperty("accountNumber", result.get("_id").toString());
		exchange.setProperty("channelName", "lazada");
	}

	private void getCommonAccountDetails(Exchange exchange, List<DBObject> results, String sellerID) {
		for (DBObject result : results) {
			List<DBObject> lazadaObjList = (List<DBObject>) result.get("lazada");
			DBObject lazadaObj = LazadaUtil.getChannelObjBySellerID(lazadaObjList, sellerID);
			if (lazadaObj.containsField("status") && lazadaObj.get("status").toString().equals("X")) {
				results.remove(result);
			} else {
				result.put("lazada", lazadaObj);
			}
		}
		exchange.setProperty("commonAccountDetails", results);
	}

}
