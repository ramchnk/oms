package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;

import net.spy.memcached.MemcachedClient;

public class InitialiseGetItemPriceRoute implements Processor {
	static Logger log = Logger.getLogger(InitialiseGetItemPriceRoute.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		String itemID = inBody.getString("itemID");
		exchange.setProperty("itemID", itemID);
		exchange.setProperty("refreshAccessToken", false);
		String countryCode = inBody.getString("countryCode");
		exchange.setProperty("countryCode", countryCode);
		String accessToken = getAccessToken(countryCode);
		if (accessToken == null) {
			log.info("accessToken expired for countryCode: " + countryCode);
			String refreshToken = Config.getConfig().getCommonRefreshToken(countryCode);
			exchange.setProperty("refreshToken", refreshToken);
			exchange.setProperty("refreshAccessToken", true);
			return;
		}
		exchange.setProperty("accessToken", accessToken);
	}

	private String getAccessToken(String countryCode) {
		return (String) LazadaUtil.getValueFromMemcache(countryCode + "-common-accessToken", true);
	}
}
