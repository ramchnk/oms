package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mudra.sellinall.config.APIUrlConfig;

public class InitializeAccountSplitter implements Processor {
	static Logger log = Logger.getLogger(InitializeAccountSplitter.class.getName());

	public void process(Exchange exchange) throws Exception {
		DBObject user = exchange.getIn().getBody(DBObject.class);
		log.debug("InitializeItemSyncMessageListnerRoute Received body: " + user);
		exchange.getOut().setBody(user);

		BasicDBObject channel = (BasicDBObject) user.get("lazada");
		BasicDBObject lazadaPostHelper = (BasicDBObject) channel.get("postHelper");
		String userID = lazadaPostHelper.getString("userID");
		boolean isAutoStatusUpdate = false;
		if (channel.containsField("isAutoStatusUpdate")) {
			isAutoStatusUpdate = channel.getBoolean("isAutoStatusUpdate");
		}

		boolean isPartialAutoStatusUpdate = true;
		boolean isGlobalAccount = false;
		if (channel.containsField("isPartialAutoStatusUpdate")) {
			isPartialAutoStatusUpdate = channel.getBoolean("isPartialAutoStatusUpdate");
		}
		if (channel.getString("countryCode").equals("GLOBAL")) {
			isGlobalAccount = true;
		}

		exchange.setProperty("countryCode", channel.getString("countryCode").toUpperCase());
		exchange.setProperty("hostURL", APIUrlConfig.getNewAPIUrl(channel.getString("countryCode").toUpperCase()));
		exchange.setProperty("userID", userID);
		exchange.setProperty("UserDetails", user);
		exchange.setProperty("nickNameID", getNicknameId(user, "lazada"));
		exchange.setProperty("merchantID", user.get("merchantID"));
		exchange.setProperty("refreshToken", lazadaPostHelper.getString("refreshToken"));
		exchange.setProperty("isAutoStatusUpdate", isAutoStatusUpdate);
		exchange.setProperty("isPartialAutoStatusUpdate", isPartialAutoStatusUpdate);
		exchange.setProperty("isGlobalAccount", isGlobalAccount);
	}

	private String getNicknameId(DBObject userDetails, String channelName) {
		BasicDBObject channel = (BasicDBObject) userDetails.get(channelName);
		BasicDBObject nickName = (BasicDBObject) channel.get("nickName");
		return nickName.getString("id");
	}
}
