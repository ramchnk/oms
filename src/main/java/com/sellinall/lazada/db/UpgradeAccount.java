package com.sellinall.lazada.db;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.config.APIUrlConfig;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.util.DateUtil;
import com.sellinall.util.EncryptionUtil;

public class UpgradeAccount implements Processor{
	static Logger log = Logger.getLogger(UpgradeAccount.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject userCredentials = exchange.getProperty("userCredentials", JSONObject.class);
		String nickNameID = "";
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		BasicDBObject searchQuery = new BasicDBObject("_id", new ObjectId(accountNumber));
		if (exchange.getProperties().containsKey("nickNameID")) {
			nickNameID = exchange.getProperty("nickNameID", String.class);
			searchQuery.put("lazada.nickName.id", nickNameID);
		}
		BasicDBObject upgradeData = createUpgradeData(userCredentials);
		if (upgradeData == null) {
			exchange.setProperty("failureReason", "Update failed");
			return;
		}
		log.info("Successfully upgraded to new API for accountNumber : " + accountNumber + ", nickNameID : " + nickNameID);
		DBCollection table = DbUtilities.getDBCollection("accounts");
		DBObject accountDetails = table.findAndModify(searchQuery, new BasicDBObject("lazada", 1), null, false,
				new BasicDBObject("$set", upgradeData), true, false);
		List<DBObject> lazadaObjList = (List<DBObject>) accountDetails.get("lazada");
		boolean processOrdersWithSKUOnly = false;
		for (DBObject lazadaObj : lazadaObjList) {
			BasicDBObject nickNameObj = (BasicDBObject) lazadaObj.get("nickName");
			if (nickNameID.equals(nickNameObj.get("id").toString())
					&& lazadaObj.containsField("processOrdersWithSKUOnly")) {
				processOrdersWithSKUOnly =  (Boolean) lazadaObj.get("processOrdersWithSKUOnly");
			}
		}
		if (processOrdersWithSKUOnly) {
			checkForCommonStoreAccounts(userCredentials, upgradeData);
		}
	}

	private void checkForCommonStoreAccounts(JSONObject userCredentials, BasicDBObject upgradeData)
			throws JSONException {
		JSONArray countryUserInfo = userCredentials.getJSONArray("country_user_info");
		JSONObject userDetails = countryUserInfo.getJSONObject(0);
		String userID = userDetails.getString("user_id");
		DBCollection table = DbUtilities.getDBCollection("accounts");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("lazada.postHelper.userID", userID);
		BasicDBObject projection = new BasicDBObject();
		projection.put("lazada.$", 1);
		List<DBObject> accountList = table.find(searchQuery, projection).toArray();
		for (DBObject accountObj : accountList) {
			DBObject lazadaObj = ((List<DBObject>) accountObj.get("lazada")).get(0);
			BasicDBObject nickNameObj = (BasicDBObject) lazadaObj.get("nickName");
			if (lazadaObj.containsField("postHelper")) {
				BasicDBObject postHelper = (BasicDBObject) lazadaObj.get("postHelper");
				if (postHelper.containsField("userID") && postHelper.getString("userID").equals(userID)) {
					updateRefreshTokenForCommonStoreAccount(upgradeData, nickNameObj, accountObj.get("_id").toString());
				}
			}
		}
	}

	private void updateRefreshTokenForCommonStoreAccount(BasicDBObject upgradeData, BasicDBObject nickNameObj,
			String accountNumber) {
		if (upgradeData == null) {
			log.error("Failed to authorize for accountNumber : " + accountNumber + ", nickNameID : "
					+ nickNameObj.getString("id"));
			return;
		}
		log.info("Successfully authorized lazada account for accountNumber : " + accountNumber + ", nickNameID : "
				+ nickNameObj.getString("id"));
		BasicDBObject searchQuery = new BasicDBObject("_id", new ObjectId(accountNumber));
		searchQuery.put("lazada.nickName.id", nickNameObj.getString("id"));
		DBCollection table = DbUtilities.getDBCollection("accounts");
		table.update(searchQuery, new BasicDBObject("$set", upgradeData));
	}

	private static BasicDBObject createUpgradeData(JSONObject userCredentials) {
		BasicDBObject newChannel = new BasicDBObject();
		try {
			JSONArray countryUserInfo = userCredentials.getJSONArray("country_user_info");
			JSONObject userDetails = countryUserInfo.getJSONObject(0);
			String hostUrl = APIUrlConfig.getNewAPIUrl(userDetails.getString("country").toUpperCase());
			BasicDBObject postHelper = new BasicDBObject();
			postHelper.put("hostURL", hostUrl);
			long currentTime = (Long) DateUtil.getSIADateFormat();
			long refreshTokenExipryDate = userCredentials.getLong("refresh_expires_in") + currentTime;
			postHelper.put("refreshToken", EncryptionUtil.encrypt(userCredentials.getString("refresh_token")));
			postHelper.put("refreshTokenExipryDate", refreshTokenExipryDate);
			postHelper.put("expiresIn", userCredentials.get("expires_in"));
			postHelper.put("userID", userDetails.getString("user_id"));
			newChannel.put("lazada.$.postHelper", postHelper);
			newChannel.put("lazada.$.oauth2Authenticated", true);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return newChannel;
	}
}
