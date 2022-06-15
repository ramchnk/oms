package com.sellinall.lazada.requests;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.mudra.sellinall.config.Config;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;


public class GetAccessToken implements Processor {
	static Logger log = Logger.getLogger(GetAccessToken.class.getName());

	public void process(Exchange exchange) throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		String authToken = exchange.getProperty("authToken", String.class);
		map.put("code", authToken);
		String payload = "code=" + authToken;
		String clientID = "";
		String clientSecret = "";
		boolean isChatApp = false;
		if (exchange.getProperties().containsKey("appType") && exchange.getProperty("appType").equals("chat")) {
			isChatApp = true;
			clientID = Config.getConfig().getLazadaChatClientID();
			clientSecret = Config.getConfig().getLazadaChatClientSecret();
		} else {
			clientID = Config.getConfig().getLazadaClientID();
			clientSecret = Config.getConfig().getLazadaClientSecret();
		}
		String response = NewLazadaConnectionUtil.callAPI(Config.getConfig().getCompleteAuthUrl(), "/auth/token/create",
				null, map, payload, "", "POST", clientID, clientSecret);

		JSONObject responseObj = new JSONObject(response);
		if (!responseObj.has("access_token")) {
			log.error("Failed to get access token, request : " + payload + " & response : " + responseObj);
			exchange.setProperty("failureReason", "Auth failure please check your credential");
			return;
		}
		if (!responseObj.has("country_user_info") && responseObj.has("country_user_info_list")) {
			responseObj.put("country_user_info", responseObj.get("country_user_info_list"));
		}
		boolean isGlobalAccount = false;
		if (responseObj.has("country") && responseObj.getString("country").equalsIgnoreCase("cb")) {
			isGlobalAccount = true;
			log.info("Global account found for accountNumber : " + exchange.getProperty("accountNumber")
					+ " & country user info : " + responseObj.get("country_user_info"));
		}
		exchange.setProperty("isGlobalAccount", isGlobalAccount);
		exchange.setProperty("userCredentials", responseObj);
		exchange.setProperty("isValidAccount", true);
		// For only upgrade account flow,nickNameID will be available
		if (exchange.getProperties().containsKey("nickNameID") && !isValidAccount(exchange, isChatApp)) {
			exchange.setProperty("failureReason", "please authorize with valid account");
			exchange.setProperty("isValidAccount", false);
		}
	}

	private Boolean isValidAccount(Exchange exchange, boolean isChatApp) throws JSONException {
		DBCollection table = DbUtilities.getDBCollection("accounts");
		BasicDBObject searchQuery = new BasicDBObject();
		ObjectId objId = new ObjectId(exchange.getProperty("accountNumber", String.class));
		searchQuery.put("_id", objId);
		searchQuery.put("lazada.nickName.id", exchange.getProperty("nickNameID", String.class));
		BasicDBObject projectionQuery = new BasicDBObject();
		projectionQuery.put("lazada.$.postHelper", 1);
		DBObject queryResult = table.findOne(searchQuery, projectionQuery);
		// compares userID from DB and response to validate correct credentials
		// for re-Authorise the existing account
		if (queryResult != null && queryResult.containsKey("lazada")) {
			List<DBObject> lazadaObject = (List<DBObject>) queryResult.get("lazada");
			DBObject lazadaObj = (DBObject) lazadaObject.get(0);
			if (lazadaObj.containsKey("postHelper")) {
				DBObject lazadaPostHelper = (DBObject) lazadaObj.get("postHelper");
				JSONObject userCredentials = exchange.getProperty("userCredentials", JSONObject.class);
				JSONArray countryUserInfo = userCredentials.getJSONArray("country_user_info");
				JSONObject userDetails = countryUserInfo.getJSONObject(0);
				if (exchange.getProperty("isGlobalAccount", Boolean.class)) {
					List<String> sellerIDListFromMP = new ArrayList<String>();
					for (int i = 0; i < countryUserInfo.length(); i++) {
						sellerIDListFromMP.add(countryUserInfo.getJSONObject(i).getString("seller_id"));
					}

					String sellerID = "";
					if (lazadaPostHelper.containsField("sellerIDList")) {
						List<DBObject> sellerIDObjList = (List<DBObject>) lazadaPostHelper.get("sellerIDList");
						sellerID = sellerIDObjList.get(0).get("sellerID").toString();
					} else if (lazadaPostHelper.containsField("sellerID")) {
						sellerID = lazadaPostHelper.get("sellerID").toString();
					}

					if (sellerIDListFromMP.contains(sellerID)) {
						return true;
					} else {
						log.error("reAuth failed for " + exchange.getProperty("accountNumber", String.class)
								+ " nickNameID: " + exchange.getProperty("nickNameID", String.class)
								+ " sellerID from DB: " + sellerID + " sellerID from Response " + sellerIDListFromMP);
					}
				}
				if (lazadaPostHelper.containsField("sellerID")) {
					if (lazadaPostHelper.get("sellerID").toString().equals(userDetails.getString("seller_id"))) {
						return true;
					} else {
						log.error("reAuth failed for " + exchange.getProperty("accountNumber", String.class)
								+ " nickNameID: " + exchange.getProperty("nickNameID", String.class)
								+ " sellerID from DB: " + lazadaPostHelper.get("sellerID").toString()
								+ " sellerID from Response " + userDetails.getString("seller_id"));
					}
				}
				if (lazadaPostHelper.get("userID").toString().equals(userDetails.getString("user_id"))) {
					return true;
				} else {
					log.error("reAuth failed for " + exchange.getProperty("accountNumber", String.class)
							+ " nickNameID: " + exchange.getProperty("nickNameID", String.class) + " userID from DB: "
							+ lazadaPostHelper.get("userID").toString() + " userID from Response "
							+ userDetails.getString("user_id"));
				}
			}
		}
		return false;
	}
}
