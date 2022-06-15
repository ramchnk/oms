package com.sellinall.lazada.db;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mudra.sellinall.database.DbUtilities;

public class UpdateUserPolices implements Processor {
	static Logger log = Logger.getLogger(UpdateUserPolices.class.getName());

	public void process(Exchange exchange) throws Exception {
		DBCollection table = DbUtilities.getDBCollection("accounts");
		DBObject update = new BasicDBObject();
		String nickNameId = exchange.getProperty("nickNameID", String.class);
		String channelName = nickNameId.split("-")[0];
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		DBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", new ObjectId(accountNumber));
		searchQuery.put(channelName + ".nickName.id", nickNameId);
		Object shippingProviders = exchange.getIn().getBody();
		if (shippingProviders instanceof ArrayList) {
			shippingProviders = exchange.getIn().getBody(ArrayList.class);
		} else {
			JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
			if (inBody.get("shippingServiceProvider") instanceof JSONArray) {
				shippingProviders = (BasicDBList) JSON.parse(inBody.getJSONArray("shippingServiceProvider").toString());
			} else if (inBody.get("shippingServiceProvider") instanceof JSONObject) {
				shippingProviders = (DBObject) JSON.parse(inBody.getJSONObject("shippingServiceProvider").toString());
			}
		}
		if (exchange.getProperties().containsKey("isGlobalAccount")
				&& exchange.getProperty("isGlobalAccount", Boolean.class)) {
			String countryCode = exchange.getProperty("syncCountryCode", String.class);
			Boolean isUpdatePolicies = exchange.getProperty("isUpdatePolicies", Boolean.class);
			if (isUpdatePolicies) {
				update.put(channelName + ".$.shippingServiceProvider", shippingProviders);
			} else {
				update.put(channelName + ".$.shippingServiceProvider." + countryCode, shippingProviders);
			}
		} else {
			update.put(channelName + ".$.shippingServiceProvider", shippingProviders);
		}
		table.update(searchQuery, new BasicDBObject("$set", update));
		JSONObject response = new JSONObject();
		response.put("response", "success");
		exchange.getOut().setBody(response);
	}
}