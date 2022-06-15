package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;

import net.spy.memcached.MemcachedClient;

public class UpdateCommonAccessToken implements Processor {

	static Logger log = Logger.getLogger(UpdateCommonAccessToken.class.getName());
	static int THIRTY_FIVE_MINUTES = 35 * 60;

	public void process(Exchange exchange) throws Exception {
		JSONObject responseObj = exchange.getProperty("userCredentials", JSONObject.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String countryCode = exchange.getProperty("countryCode", String.class);
		MemcachedClient mc = LazadaUtil.getMemcachedClient();
		int expireTime = (int) (responseObj.getLong("expires_in") - THIRTY_FIVE_MINUTES);
		mc.set(countryCode + "-common-accessToken", expireTime, accessToken);
	}
}
