package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.util.EncryptionUtil;

public class UpdateRefreshToken implements Processor {
	public void process(Exchange exchange) throws Exception {
		JSONObject userCredentials = exchange.getProperty("userCredentials", JSONObject.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameId = exchange.getProperty("nickNameID", String.class);
		Long expireTime = exchange.getProperty("refreshExpiresIn", Long.class);
		String appType = exchange.getProperties().containsKey("appType") ? (String) exchange.getProperty("appType")
				: "";
		JSONArray country_user_info = userCredentials.getJSONArray("country_user_info");
		JSONObject userDetails = country_user_info.getJSONObject(0);

		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", new ObjectId(accountNumber));
		searchQuery.put("lazada.nickName.id", nickNameId);
		BasicDBObject updateObj = new BasicDBObject();
		updateObj.put("lazada.$.postHelper.sellerID", userDetails.getString("seller_id"));
		updateObj.put("lazada.$.postHelper." + appType + "RefreshToken", EncryptionUtil.encrypt(userCredentials.getString("refresh_token")));
		updateObj.put("lazada.$.postHelper.chatRefreshExpiresIn", expireTime);
		updateObj.put("lazada.$." + appType + "RefreshTokenUpdatedTime", System.currentTimeMillis() / 1000);
		DBCollection table = DbUtilities.getDBCollection("accounts");
		DBObject result = table.findAndModify(searchQuery, new BasicDBObject("$set", updateObj));
		if (result != null) {
			if (result.containsField("merchantID")) {
				exchange.setProperty("merchantID", result.get("merchantID").toString());
			}
		}
	}
}
