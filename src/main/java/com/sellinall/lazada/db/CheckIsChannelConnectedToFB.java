package com.sellinall.lazada.db;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class CheckIsChannelConnectedToFB implements Processor {

	public void process(Exchange exchange) throws Exception {
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		exchange.setProperty("isNickNameIDConnectedToFB", isNickIDConnectedToFb(exchange, nickNameID));
	}

	private boolean isNickIDConnectedToFb(Exchange exchange, String nickNameID) {
		DBObject user = exchange.getProperty("UserDetails", DBObject.class);
		ArrayList<BasicDBObject> fbUser = (ArrayList<BasicDBObject>) user.get("faceBook");
		BasicDBObject faceBook = fbUser.get(0);
		Object object = faceBook.get("connectTo");
		if (object instanceof BasicDBObject) {
			// Existing user have like this format only
			BasicDBObject connectTo = (BasicDBObject) faceBook.get("connectTo");
			if (nickNameID.equals(connectTo.getString("id"))) {
				return true;
			}
			return false;
		}
		ArrayList<String> connectTo = (ArrayList<String>) faceBook.get("connectTo");
		if (connectTo.contains(nickNameID)) {
			return true;
		}
		return false;
	}
}
