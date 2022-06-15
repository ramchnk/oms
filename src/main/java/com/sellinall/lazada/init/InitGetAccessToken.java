package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.sellinall.lazada.util.LazadaUtil;

import net.spy.memcached.MemcachedClient;

public class InitGetAccessToken implements Processor {
	static Logger log = Logger.getLogger(InitGetAccessToken.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String appType = "";
		if (exchange.getProperties().containsKey("appType")) {
			appType = exchange.getProperty("appType", String.class);
		}
		exchange.setProperty("refreshAccessToken", false);
		String accessToken = getAccessToken(accountNumber, nickNameID,true, appType);
		if (accessToken == null) {
			log.info("accessToken expired for accountNumber: " + accountNumber + "for nickname: " + nickNameID);
			exchange.setProperty("refreshAccessToken", true);
			return;
		}
		exchange.setProperty("accessToken", accessToken);
	}

	private String getAccessToken(String accountNumber, String nickNameID, boolean retry, String appType) {
		MemcachedClient mc = LazadaUtil.getMemcachedClient();
		String accessToken = null;
		try {
			Object mcValue = null;
			if (!appType.isEmpty()) {
				mcValue = mc.get(accountNumber + "-" + nickNameID + "-" + appType + "-accessToken");
			} else {
				mcValue = mc.get(accountNumber + "-" + nickNameID + "-accessToken");
			}
			if (mcValue != null) {
				accessToken = (String) mcValue;
			}
		} catch (Exception e) {
			if (retry) {
				try {
					log.error("Retrying to read value from  memcache,key:"+ accountNumber + "-"  + nickNameID + "-accessToken");
					mc.shutdown();
					LazadaUtil.initMemoryCached();
					accessToken = getAccessToken(accountNumber, nickNameID, false, appType);
				} catch (Exception i) {
					i.printStackTrace();
				}
			} else {
				log.error("unable to get access token for accountNumber: " + accountNumber + "for nickname: "
						+ nickNameID, e);
			}
		}
		return accessToken;
	}
}
