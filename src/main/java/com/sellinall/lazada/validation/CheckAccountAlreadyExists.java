package com.sellinall.lazada.validation;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;

public class CheckAccountAlreadyExists implements Processor {

	public void process(Exchange exchange) throws Exception {
		exchange.setProperty("accountAlreadyExists", false);
		JSONObject userCredentials = exchange.getProperty("userCredentials", JSONObject.class);
		boolean isGlobalAccount = false;
		if (exchange.getProperties().containsKey("isGlobalAccount")) {
			isGlobalAccount = exchange.getProperty("isGlobalAccount", Boolean.class);
		}
		boolean isAccountExists = checkAccountAlreadyExists(userCredentials, isGlobalAccount);
		if (isAccountExists) {
			exchange.setProperty("accountAlreadyExists", true);
			exchange.setProperty("failureReason", "Account already exists");
		}
	}

	private static boolean checkAccountAlreadyExists(JSONObject userCredentials, boolean isGlobalAccount)
			throws Exception {
		DBCollection table = DbUtilities.getDBCollection("accounts");
		BasicDBObject searchQuery = new BasicDBObject();
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("userID", userCredentials.getString("account"));
		if (isGlobalAccount) {
			elemMatch.put("countryCode", "GLOBAL");
		} else {
			elemMatch.put("countryCode", userCredentials.getString("country").toUpperCase());
		}
		searchQuery.put("lazada", new BasicDBObject("$elemMatch", elemMatch));
		BasicDBObject projection = new BasicDBObject("_id", 1);
		List<DBObject> accountList = table.find(searchQuery, projection).toArray();
		if (accountList.size() > 0) {
			return true;
		}
		return false;
	}
}
