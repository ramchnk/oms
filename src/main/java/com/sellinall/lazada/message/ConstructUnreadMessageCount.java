package com.sellinall.lazada.message;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.lazada.util.LazadaUtil;

public class ConstructUnreadMessageCount implements Processor {
	static Logger log = Logger.getLogger(ConstructUnreadMessageCount.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject message = exchange.getIn().getBody(JSONObject.class);
		BasicDBObject accountDetails = (BasicDBObject) LazadaUtil.getAccountDetails(message.getString("seller_id"));
		if (accountDetails == null) {
			exchange.setProperty("isEligibleToPublishMsg", false);
			return;
		}
		exchange.setProperty("isEligibleToPublishMsg", true);
		List<DBObject> lazada = (List<DBObject>) accountDetails.get("lazada");
		DBObject lazadaObj = lazada.get(0);
		String countryCode = lazadaObj.get("countryCode").toString();
		DBObject nickNameObj = (DBObject) lazadaObj.get("nickName");

		JSONObject unreadCountMessage = new JSONObject();
		unreadCountMessage.put("merchantID", accountDetails.getString("merchantID"));
		unreadCountMessage.put("countryCode", countryCode);
		unreadCountMessage.put("accountNumber", accountDetails.getString("_id"));
		unreadCountMessage.put("nickNameID", nickNameObj.get("id").toString());
		unreadCountMessage.put("requestType", "updateInquirySummary");

		if (message.has("data")) {
			JSONObject data = message.getJSONObject("data");
			if (data.has("from_account_type")) {
				exchange.setProperty("fromAccountType", data.get("from_account_type"));
				unreadCountMessage.put("fromAccountType", data.get("from_account_type"));
			}
			if (data.has("sync_type") && data.get("sync_type").equals("SESSION_UPDATE")) {
				exchange.setProperty("syncType", data.get("sync_type"));
				unreadCountMessage.put("syncType", data.get("sync_type"));
				unreadCountMessage.put("unread_count", data.get("unread_count"));
			}
			if (data.has("session_id")) {
				unreadCountMessage.put("sessionID", data.get("session_id"));
			}
		}
		log.debug(unreadCountMessage);
		exchange.getOut().setBody(unreadCountMessage);

	}

}
