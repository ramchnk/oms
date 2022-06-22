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
		String accountNumber = "CEO";

		boolean isPublishItemSyncMessage = true;
		int lazadaAccountSize = 0;

		exchange.setProperty("needToPublishItemSyncMsg", isPublishItemSyncMessage);

		JSONObject request = exchange.getProperty("userCredentials", JSONObject.class);
		BasicDBObject accountData = createAccountData(request, lazadaAccountSize);
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
		searchQuery.put("merchantID", accountNumber);
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("userID", request.getString("account"));

		elemMatch.put("countryCode", userDetails.getString("country").toUpperCase());

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
			table.update(new BasicDBObject("merchantID", accountNumber),
					new BasicDBObject("$push", accountData));
		}

	}

	private static BasicDBObject createAccountData(JSONObject userCredentials, int lazadaAccountSize) {
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

			hostUrl = APIUrlConfig.getNewAPIUrl(userDetails.getString("country").toUpperCase());
			ArrayList<DBObject> shippingServiceProvider = null;
			newChannel.put("countryCode", userDetails.getString("country").toUpperCase());
			newChannel.put("currencyCode",
					Config.getConfig().getCurrencyCode(userDetails.getString("country").toUpperCase()));
			shippingServiceProvider = GetPolicies.GetShippingProviderDetails(userCredentials.getString("access_token"),
					hostUrl);
			newChannel.put("shippingServiceProvider", shippingServiceProvider);

			newChannel.put("postHelper", constructPostHelper(userCredentials, hostUrl));
			constructDefualtValues(newChannel);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		update.put("lazada", newChannel);
		return update;
	}

	private static void constructDefualtValues(BasicDBObject newChannel) {
		newChannel.put("enablePost", true);
		newChannel.put("oauth2Authenticated", true);
		newChannel.put("invoiceTemplate", "1");
		newChannel.put("descriptionTemplate", "none");
		newChannel.put("lastNewOrderScannedTime", (Long) DateUtil.getSIADateFormat());
		newChannel.put("lastUpdatedOrderScannedTime", (Long) DateUtil.getSIADateFormat());
		newChannel.put("timeLinked", (Long) DateUtil.getSIADateFormat());
	}

	private static BasicDBObject constructPostHelper(JSONObject userCredentials, String hostUrl) throws JSONException {
		JSONArray country_user_info = userCredentials.getJSONArray("country_user_info");
		JSONObject userDetails = country_user_info.getJSONObject(0);
		long currentTime = (Long) DateUtil.getSIADateFormat();
		long refreshTokenExipryDate = userCredentials.getLong("refresh_expires_in") + currentTime;

		BasicDBObject postHelper = new BasicDBObject();
		postHelper.put("refreshToken", EncryptionUtil.encrypt(userCredentials.getString("refresh_token")));
		postHelper.put("refreshTokenExipryDate", refreshTokenExipryDate);
		postHelper.put("expiresIn", userCredentials.get("expires_in"));
		postHelper.put("userID", userDetails.getString("user_id"));
		postHelper.put("sellerID", userDetails.getString("seller_id"));

		if (!hostUrl.isEmpty()) {
			postHelper.put("hostURL", hostUrl);
		}
		return postHelper;
	}
}