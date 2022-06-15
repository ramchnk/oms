package com.sellinall.lazada.db;

import java.util.ArrayList;

import net.spy.memcached.MemcachedClient;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;

public class UpdateSKUToMemoryCache implements Processor {
	static Logger log = Logger.getLogger(UpdateSKUToMemoryCache.class.getName());
	// 5 days 24 hours 60 M 60 S
	public static final int MC_MAX_EXPIRE_TIME = 432000;

	public void process(Exchange exchange) throws Exception {
		JSONObject inventory = exchange.getProperty("inventory", JSONObject.class);
		String SKU = inventory.getString("SKU");
		String channelName = exchange.getProperty("channelName", String.class);
		JSONObject channel = inventory.getJSONObject(channelName);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String MCKey = LazadaUtil.getMCkeyforGcStatusWaitingSKUS(channelName, accountNumber,
				channel.getString("nickNameID"));
		log.debug("MC Key = " + MCKey);
		MemcachedClient mc = LazadaUtil.getMemcachedClient();
		ArrayList<String> SKUs = (ArrayList<String>) LazadaUtil.getValueFromMemcache(MCKey, true);
		if (SKUs != null) {
			SKUs.add(SKU);
			mc.set(MCKey, MC_MAX_EXPIRE_TIME, SKUs);
			return;
		}
		// If First time then flow will come here
		SKUs = new ArrayList<String>();
		SKUs.add(SKU);
		log.debug("MC SKUS = " + SKUs.toString());
		mc.set(MCKey, MC_MAX_EXPIRE_TIME, SKUs);
	}
}
