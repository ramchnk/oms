package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;

import net.spy.memcached.MemcachedClient;

public class UpdateSOFToken implements Processor {

	static Logger log = Logger.getLogger(UpdateSOFToken.class.getName());
	static int FIFTY_FIVE_MINUTES = 55 * 60;

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String token = exchange.getProperty("sofToken", String.class);
		if (token == null) {
			log.error("updating SOFtoken failed for " + accountNumber + ": " + nickNameID + " account");
			return;
		}

		MemcachedClient mc = LazadaUtil.getMemcachedClient();
		mc.set(accountNumber + "-" + nickNameID + "-sofToken", FIFTY_FIVE_MINUTES, token);
	}

}
