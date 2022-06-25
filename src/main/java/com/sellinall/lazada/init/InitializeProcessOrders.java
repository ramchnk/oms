/**
 * 
 */
package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mudra.sellinall.config.APIUrlConfig;
import com.sellinall.lazada.util.LazadaUtil;

/**
 * @author ram
 *
 */
public class InitializeProcessOrders implements Processor {
	static Logger log = Logger.getLogger(InitializeProcessOrders.class.getName());
	static final int adjustLastScannedTimeBy = 300;

	public void process(Exchange exchange) throws Exception {
		DBObject user = exchange.getProperty("UserDetails", DBObject.class);
		log.debug("InitializeItemSyncMessageListnerRoute Received body: " + user);
		
		BasicDBList channelList = (BasicDBList) user.get("lazada");
		BasicDBObject channel = (BasicDBObject) channelList.get(0);

		String countryCode = channel.getString("countryCode");

		String hostURL = "";
		if (channel.containsField("postHelper")) {
			BasicDBObject postHelper = (BasicDBObject) channel.get("postHelper");
			exchange.setProperty("refreshToken", postHelper.getString("refreshToken"));
			hostURL = APIUrlConfig.getConfig().getNewAPIUrl(countryCode);
			exchange.setProperty("getAccessToken", true);
		} else {
			log.error("postHelper not found for this account: " + exchange.getProperty("accountNumber")
					+ " nickNameId: " );
		}
		
		exchange.setProperty("pageOffSet", 0);
		exchange.setProperty("hasMoreRecords", true);
		exchange.setProperty("hostURL", hostURL);
		exchange.setProperty("merchantID", user.get("merchantID"));
		exchange.setProperty("currencyCode", getCurrencyCode(channel, countryCode));
		exchange.setProperty("countryCode", countryCode);
		exchange.setProperty("channel", channel);
	}

	private String getCurrencyCode(BasicDBObject channel, String countryCode) {
		if (countryCode.equals("GLOBAL")) {
			return LazadaUtil.countryCodeToCurrencyMap.get(countryCode);
		}
		return channel.getString("currencyCode");
	}
}