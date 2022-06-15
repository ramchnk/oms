package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.sellinall.lazada.util.LazadaUtil;

import net.spy.memcached.MemcachedClient;

public class InitGetSOFToken implements Processor {
	static Logger log = Logger.getLogger(InitGetSOFToken.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		exchange.setProperty("refreshSOFToken", false);
		String sofToken = getSOFToken(accountNumber, nickNameID);
		if (sofToken == null) {
			log.info("sofToken expired for account: " + accountNumber + ", for nicknameID: " + nickNameID);
			exchange.setProperty("refreshSOFToken", true);
			return;
		}
		exchange.setProperty("sofToken", sofToken);
	}

	private String getSOFToken(String accountNumber, String nickNameID) {
		return (String) LazadaUtil.getValueFromMemcache(accountNumber + "-" + nickNameID + "-sofToken", true);
	}

}
