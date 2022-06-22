package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.sellinall.lazada.util.LazadaUtil;

import net.spy.memcached.MemcachedClient;

public class InitGetAccessToken implements Processor {
	static Logger log = Logger.getLogger(InitGetAccessToken.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accountNumber = "dormx";

		exchange.setProperty("refreshAccessToken", false);
		String accessToken = getAccessToken(accountNumber, true);
		if (accessToken == null) {
			log.info("accessToken expired for accountNumber: " + accountNumber);
			exchange.setProperty("refreshAccessToken", true);
			return;
		}
		exchange.setProperty("accessToken", accessToken);
	}

	private String getAccessToken(String accountNumber, boolean retry) {
		MemcachedClient mc = LazadaUtil.getMemcachedClient();
		String accessToken = null;
		try {
			Object mcValue = null;

			mcValue = mc.get(accountNumber + "-accessToken");

			if (mcValue != null) {
				accessToken = (String) mcValue;
			}
		} catch (Exception e) {
			if (retry) {
				try {
					log.error("Retrying to read value from  memcache,key:" + accountNumber + "-accessToken");
					mc.shutdown();
					LazadaUtil.initMemoryCached();
					accessToken = getAccessToken(accountNumber, false);
				} catch (Exception i) {
					i.printStackTrace();
				}
			} else {
				log.error("unable to get access token for accountNumber: " + accountNumber + "for nickname: ", e);
			}
		}
		return accessToken;
	}
}
