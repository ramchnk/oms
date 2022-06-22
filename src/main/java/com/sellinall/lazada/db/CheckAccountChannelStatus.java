package com.sellinall.lazada.db;

import java.util.ArrayList;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class CheckAccountChannelStatus implements Processor {

	static Logger log = Logger.getLogger(CheckAccountChannelStatus.class.getName());

	public void process(Exchange exchange) throws Exception {
		DBObject account = exchange.getProperty("UserDetails", DBObject.class);
		exchange.setProperty("ordersList", new ArrayList<JSONObject>());
		account.put("channelName", "lazada");
		exchange.setProperty("UserDetails", account);
		if (!account.containsField("lazada")) {
			exchange.setProperty("accountHasEligiblityToSync", false);
			return;
		}
		ArrayList<BasicDBObject> channelList = (ArrayList<BasicDBObject>) account.get("lazada");
		exchange.setProperty("accountHasEligiblityToSync", checkEligiblity(channelList));
	}

	public boolean checkEligiblity(ArrayList<BasicDBObject> channelList) {
		// If any one object true then we will proceed
		for (BasicDBObject channel : channelList) {
			if (!channel.containsField("status")) {
				return true;
			}
			String status = channel.getString("status");
			if (!status.equals("X")) {
				return true;
			}
		}
		return false;
	}

}
