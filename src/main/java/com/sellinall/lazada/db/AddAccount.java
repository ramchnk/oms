package com.sellinall.lazada.db;

import java.util.ArrayList;
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
import com.mudra.sellinall.config.Config;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.lazada.requests.GetPolicies;
import com.sellinall.util.DateUtil;
import com.sellinall.util.EncryptionUtil;

public class AddAccount implements Processor {
	static Logger log = Logger.getLogger(AddAccount.class.getName());
	static final int FIVE_MIN_IN_SEC = 5 * 60;

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		BasicDBObject accountDetails = getAccountDetails(accountNumber);
		boolean isPublishItemSyncMessage = true;
		int lazadaAccountSize = 0;
		if (accountDetails.containsField("lazada")) {
			ArrayList<BasicDBObject> lazada = (ArrayList<BasicDBObject>) accountDetails.get("lazada");
			isPublishItemSyncMessage = checkIsEligibleToPublishMsg(lazada);
			lazadaAccountSize = lazada.size();
		}
		boolean isGlobalAccount = false;
		if (exchange.getProperties().containsKey("isGlobalAccount")) {
			isGlobalAccount = exchange.getProperty("isGlobalAccount", Boolean.class);
		}
		exchange.setProperty("needToPublishItemSyncMsg", isPublishItemSyncMessage);

		List<String> wmsList = getWMSList(accountDetails);
		JSONObject request = exchange.getProperty("userCredentials", JSONObject.class);
		BasicDBObject accountData = createAccountData(request, lazadaAccountSize, wmsList, isGlobalAccount);
		exchange.setProperty("isValidAccount", false);
		if (accountData == null) {
			exchange.setProperty("failureReason", "Auth failure");
			return;
		}
		exchange.setProperty("isValidAccount", true);
		JSONArray countryUserInfo = request.getJSONArray("country_user_info");
		JSONObject userDetails = countryUserInfo.getJSONObject(0);
		DBCollection table = DbUtilities.getDBCollection("accounts");
		DBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", new ObjectId(accountNumber));
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("userID", request.getString("account"));
		if (isGlobalAccount) {
			elemMatch.put("countryCode", "GLOBAL");
		} else {
			elemMatch.put("countryCode", userDetails.getString("country").toUpperCase());
		}
		searchQuery.put("lazada", new BasicDBObject("$elemMatch", elemMatch));

		DBObject accountDetail = table.findOne(searchQuery);
		exchange.setProperty("stopProcess", false);
		if (accountDetail != null) {
			log.warn("Multiple call coming parallelly to link account for the userID: " + request.getString("account")
					+ " ,accountNumber: " + accountNumber);
			JSONObject response = new JSONObject();
			response.put("response", "failure");
			response.put("responseMessage", "'Account is already linked.");
			exchange.setProperty("stopProcess", true);
			exchange.getOut().setBody(response);
		} else {
			table.update(new BasicDBObject("_id", new ObjectId(accountNumber)),
					new BasicDBObject("$push", accountData));
		}

	}

	private boolean checkIsEligibleToPublishMsg(ArrayList<BasicDBObject> lazada) {
		boolean needToPublishMsg = true;
		long lastScannedTime = 0L;
		for (BasicDBObject lazadaObj : lazada) {
			if (!lazadaObj.containsField("status") || lazadaObj.getString("status").equals("A")) {
				needToPublishMsg = false;
				break;
			} else {
				if (lastScannedTime < lazadaObj.getLong("lastNewOrderScannedTime")) {
					lastScannedTime = lazadaObj.getLong("lastNewOrderScannedTime");
				}
			}
		}
		if ((System.currentTimeMillis() / 1000L) - lastScannedTime < FIVE_MIN_IN_SEC) {
			needToPublishMsg = false;
		}
		return needToPublishMsg;
	}

	private List<String> getWMSList(BasicDBObject accountDetails) {
		List<String> wmsList = new ArrayList<String>();
		if (accountDetails.containsField("MyStock")) {
			List<DBObject> myStockList = (List<DBObject>) accountDetails.get("MyStock");
			if (myStockList.size() > 0) {
				DBObject myStockObj = myStockList.get(0);
				wmsList.add(((BasicDBObject) myStockObj.get("nickName")).getString("id"));
			}
		}
		return wmsList;
	}

	private BasicDBObject getAccountDetails(String accountNumber) {
		DBCollection table = DbUtilities.getDBCollection("accounts");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", new ObjectId(accountNumber));
		BasicDBObject projection = new BasicDBObject("lazada", 1);
		projection.put("MyStock", 1);
		BasicDBObject account = (BasicDBObject) table.findOne(searchQuery, projection);
		return account;
	}

	private static BasicDBObject createAccountData(JSONObject userCredentials, int lazadaAccountSize,
			List<String> wmsList, boolean isGlobalAccount) {
		BasicDBObject update = new BasicDBObject();
		BasicDBObject newChannel = new BasicDBObject();
		try {
			BasicDBObject nickName = new BasicDBObject();
			nickName.put("id", "lazada-" + (lazadaAccountSize + 1));
			nickName.put("value", "" + (lazadaAccountSize + 1));
			newChannel.put("nickName", nickName);
			newChannel.put("userID", userCredentials.getString("account"));
			JSONArray country_user_info = userCredentials.getJSONArray("country_user_info");
			JSONObject userDetails = country_user_info.getJSONObject(0);
			String hostUrl = "";
			if (!isGlobalAccount) {
				hostUrl = APIUrlConfig.getNewAPIUrl(userDetails.getString("country").toUpperCase());
				ArrayList<DBObject> shippingServiceProvider = null;
				newChannel.put("countryCode", userDetails.getString("country").toUpperCase());
				newChannel.put("currencyCode",
						Config.getConfig().getCurrencyCode(userDetails.getString("country").toUpperCase()));
				shippingServiceProvider = GetPolicies
						.GetShippingProviderDetails(userCredentials.getString("access_token"), hostUrl);
				newChannel.put("shippingServiceProvider", shippingServiceProvider);
			} else {
				newChannel.put("countryCode", "GLOBAL");
				BasicDBObject globalShippingServiceProvider = new BasicDBObject();
				String[] globalCountryCode = Config.getConfig().getGlobalCountryCode().split("-");
				for (String countryCode : globalCountryCode) {
					hostUrl = APIUrlConfig.getNewAPIUrl(countryCode);
					globalShippingServiceProvider.put(countryCode,
							GetPolicies.GetShippingProviderDetails(userCredentials.getString("access_token"), hostUrl));
				}
				newChannel.put("shippingServiceProvider", globalShippingServiceProvider);
			}
			newChannel.put("postHelper", constructPostHelper(userCredentials, hostUrl, isGlobalAccount));
			constructDefualtValues(newChannel);
			if (wmsList.size() > 0) {
				newChannel.put("wms", wmsList);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		update.put("lazada", newChannel);
		return update;
	}

	private static void constructDefualtValues(BasicDBObject newChannel){
		newChannel.put("enablePost", true);
		newChannel.put("oauth2Authenticated", true);
		newChannel.put("invoiceTemplate", "1");
		newChannel.put("descriptionTemplate", "none");
		newChannel.put("lastNewOrderScannedTime", (Long) DateUtil.getSIADateFormat());
		newChannel.put("lastUpdatedOrderScannedTime", (Long) DateUtil.getSIADateFormat());
		newChannel.put("timeLinked", (Long) DateUtil.getSIADateFormat());
	}

	private static BasicDBObject constructPostHelper(JSONObject userCredentials, String hostUrl,
			boolean isGlobalAccount) throws JSONException {
		JSONArray country_user_info = userCredentials.getJSONArray("country_user_info");
		JSONObject userDetails = country_user_info.getJSONObject(0);
		long currentTime = (Long) DateUtil.getSIADateFormat();
		long refreshTokenExipryDate = userCredentials.getLong("refresh_expires_in") + currentTime;

		List<DBObject> sellerIDList = new ArrayList<DBObject>();
		if (isGlobalAccount) {
			for (int i = 0; i < country_user_info.length(); i++) {
				JSONObject countryUserObj = country_user_info.getJSONObject(i);

				DBObject countryObj = new BasicDBObject();
				countryObj.put("countryCode", countryUserObj.getString("country"));
				countryObj.put("userID", countryUserObj.getString("user_id"));
				countryObj.put("sellerID", countryUserObj.getString("seller_id"));

				sellerIDList.add(countryObj);
			}
		}

		BasicDBObject postHelper = new BasicDBObject();
		postHelper.put("refreshToken", EncryptionUtil.encrypt(userCredentials.getString("refresh_token")));
		postHelper.put("refreshTokenExipryDate", refreshTokenExipryDate);
		postHelper.put("expiresIn", userCredentials.get("expires_in"));
		postHelper.put("userID", userDetails.getString("user_id"));
		postHelper.put("sellerID", userDetails.getString("seller_id"));
		if (isGlobalAccount) {
			postHelper.put("sellerIDList", sellerIDList);
		}
		if (!hostUrl.isEmpty()) {
			postHelper.put("hostURL", hostUrl);
		}
		return postHelper;
	}
}