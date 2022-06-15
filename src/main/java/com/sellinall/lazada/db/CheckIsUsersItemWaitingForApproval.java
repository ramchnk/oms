package com.sellinall.lazada.db;

import java.util.ArrayList;

import net.spy.memcached.MemcachedClient;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.sellinall.lazada.util.LazadaUtil;

public class CheckIsUsersItemWaitingForApproval implements Processor {
	static Logger log = Logger.getLogger(CheckIsUsersItemWaitingForApproval.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String channelName = exchange.getProperty("channelName", String.class);
		String MCKey = LazadaUtil.getMCkeyforGcStatusWaitingSKUS(channelName, accountNumber,
				exchange.getProperty("nickNameID", String.class));
		log.debug("MCKey = " + MCKey);
		ArrayList<String> SKUs = (ArrayList<String>) LazadaUtil.getValueFromMemcache(MCKey, true);
		if (SKUs == null) {
			exchange.setProperty("isItemWaitingForApproval", false);
			return;
		}
		if (SKUs.size() == 0) {
			exchange.setProperty("isItemWaitingForApproval", false);
			return;
		}
		exchange.setProperty("isItemWaitingForApproval", true);
	}
}
