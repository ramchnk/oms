package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.APIUrlConfig;
import com.sellinall.lazada.util.LazadaUtil;

import net.sf.json.JSONArray;

public class InitializeImageUpdateRoute implements Processor {
	static Logger log = Logger.getLogger(InitializeImageUpdateRoute.class.getName());
	public void process(Exchange exchange) throws Exception {
		List<String> siteNicknameList = (List<String>) JSON.parse(exchange.getProperty("siteNicknames").toString());
		String nickNameID = siteNicknameList.get(0);
		exchange.setProperty("nickNameID", nickNameID);
		BasicDBObject userDetails = exchange.getProperty("UserDetails", BasicDBObject.class);
		ArrayList<BasicDBObject> channelList = (ArrayList<BasicDBObject>) userDetails.get("lazada");
		boolean getAccessToken = false;
		for (BasicDBObject channel : channelList) {
			BasicDBObject nickName = (BasicDBObject) channel.get("nickName");
			if (nickName.getString("id").equals(nickNameID)) {
				String hostUrl = "";
				exchange.setProperty("countryCode", channel.getString("countryCode").toUpperCase());
				if(channel.containsField("postHelper")) {
					hostUrl = APIUrlConfig.getNewAPIUrl(channel.getString("countryCode").toUpperCase());
					BasicDBObject postHelper = (BasicDBObject) channel.get("postHelper");
					exchange.setProperty("refreshToken", postHelper.getString("refreshToken"));
					getAccessToken = true;
				} else {
					log.error("postHelper not found for this account: " + exchange.getProperty("accountNumber")
							+ " nickNameID : " + nickNameID);
				}
				exchange.setProperty("hostURL", hostUrl);
				break;
			}
		}
		String timeZoneOffset = LazadaUtil.timeZoneCountryMap
				.get(exchange.getProperty("countryCode", String.class));
		exchange.setProperty("timeZoneOffset", timeZoneOffset);
		exchange.setProperty("getAccessToken", getAccessToken);
	}
}
