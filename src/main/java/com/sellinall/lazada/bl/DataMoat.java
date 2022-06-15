package com.sellinall.lazada.bl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.config.Config;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class DataMoat {
	static Logger log = Logger.getLogger(DataMoat.class.getName());

	public static JSONObject callDataMoatApi(JSONObject request, String api, String method)
			throws JSONException, UnsupportedEncodingException {
		HashMap<String, String> map = new HashMap();
		DBObject accountDetails = new BasicDBObject();
		if(request.getString("loginResult").equals("success")) {
			accountDetails = getAccountdetails(request.getString("accountNumber"));
		}
		String queryParam = constructParams(request, map, accountDetails);

		String hostUrl = Config.getConfig().getCommonApiUrl();

		JSONObject responseObj = new JSONObject();
		responseObj.put("status", "failure");
		try {
			String clientID = Config.getConfig().getCommonClientID();
			String clientSecret = Config.getConfig().getCommonClientSecret();
			if (request.has("appType") && request.getString("appType").equals("chat")) {
				clientID = Config.getConfig().getLazadaChatClientID();
				clientSecret = Config.getConfig().getLazadaChatClientSecret();
			}
			String response = NewLazadaConnectionUtil.callAPI(hostUrl, api, null, map, "", queryParam, method, clientID,
					clientSecret);
			log.info("Response for api : " + api + " & clientID : " + clientID + " is : " + response + " & request : "
					+ request);
			JSONObject serviceResponse = new JSONObject(response);
			if (serviceResponse.has("code") && serviceResponse.getString("code").equals("0")) {
				responseObj.put("status", "success");
				responseObj.put("result", serviceResponse.getJSONObject("result"));
			} else {
				responseObj.put("result", serviceResponse.getString("message"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return responseObj;
	}

	private static DBObject getAccountdetails(String accountNumber) {
		DBCollection table = DbUtilities.getDBCollection("accounts");
		BasicDBObject searchQuery = new BasicDBObject();
		ObjectId objid = new ObjectId(accountNumber);
		searchQuery.put("_id", objid);
		BasicDBObject projection = new BasicDBObject("_id", 0);
		projection.put("lazada", 1);
		DBObject object = table.findOne(searchQuery, projection);
		return object;
	}

	private static String constructParams(JSONObject payload, HashMap<String, String> map, DBObject accountDetails)
			throws JSONException, UnsupportedEncodingException {

		String queryParam = "";
		String tid = "";
		String appName = Config.getConfig().getLazadaAppName();
		if (payload.has("appType") && payload.getString("appType").equals("chat")) {
			appName = Config.getConfig().getLazadaChatAppName();
		}
		String ati = payload.getString("ati");
		String loginResult = payload.getString("loginResult");
		String time = payload.getString("time");
		String userIP = payload.getString("userIP");
		String userID = payload.getString("userID");

		if (payload.has("loginMessage")) {
			String loginMessage = payload.getString("loginMessage");
			if(loginResult.equals("success") && accountDetails.containsField("lazada")) {
				BasicDBList lazada = (BasicDBList) accountDetails.get("lazada");
				DBObject lazadaObj = (DBObject) lazada.get(0);
				tid = lazadaObj.get("userID").toString();
			} else {
				tid = userID;
			}
			map.put("loginMessage", loginMessage);
			map.put("tid", tid);
			queryParam += "&loginMessage=" + URLEncoder.encode(loginMessage, "UTF-8") + "&tid="
					+ URLEncoder.encode(tid, "UTF-8");
		}

		map.put("appName", appName);
		map.put("ati", ati);
		map.put("loginResult", loginResult);
		map.put("time", time);
		map.put("userId", userID);
		map.put("userIp", userIP);

		queryParam += "&appName=" + URLEncoder.encode(appName, "UTF-8") + "&ati=" + URLEncoder.encode(ati, "UTF-8")
				+ "&loginResult=" + URLEncoder.encode(loginResult, "UTF-8") + "&time=" + time + "&userId="
				+ URLEncoder.encode(userID, "UTF-8") + "&userIp=" + userIP;
		return queryParam;
	}
}
