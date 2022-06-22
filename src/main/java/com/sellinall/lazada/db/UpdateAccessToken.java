package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;

import net.spy.memcached.MemcachedClient;

public class UpdateAccessToken implements Processor {
	static Logger log = Logger.getLogger(UpdateAccessToken.class.getName());
	static int THIRTY_FIVE_MINUTES = 35 * 60;

	public void process(Exchange exchange) throws Exception {
		
		JSONObject responseObj = exchange.getProperty("userCredentials", JSONObject.class);
		String accessToken = responseObj.getString("access_token");
		String accountNumber = "dormx";
		
		String key = accountNumber + "-accessToken";
		if (accessToken == null) {
			exchange.setProperty("stopProcess", true);
			log.error("refresh accesstoken failed for " + key);
			return;
		} else {
			log.info("refresh accesstoken done for " + key);
		}
		MemcachedClient mc = LazadaUtil.getMemcachedClient();
		int expireTime = (int) (responseObj.getLong("expires_in") - THIRTY_FIVE_MINUTES);
		exchange.setProperty("refreshExpiresIn", responseObj.getLong("refresh_expires_in"));
		mc.set(key, expireTime, accessToken);
		if (mc.get(key) != null) {
			log.info("accesstoken updated in memcache with expire time as " + expireTime + " sec");
		} else {
			log.error("Failed to update access token for " + key);
		}
	}
}
