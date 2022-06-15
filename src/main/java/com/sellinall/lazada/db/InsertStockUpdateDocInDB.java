package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.sellinall.lazada.util.LazadaUtil;

public class InsertStockUpdateDocInDB implements Processor {
	static Logger log = Logger.getLogger(InsertStockUpdateDocInDB.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getProperty("request", JSONObject.class);
		inBody.remove("siteNicknames");
		inBody.put("requestType", "updateQuantity");
		inBody.put("needToUpdateStatus", true);
		BasicDBList updateDBList = new BasicDBList();
		updateDBList.add(BasicDBObject.parse(inBody.toString()));
		LazadaUtil.updateStockFeeds(inBody.getString("accountNumber"), inBody.getString("nickNameID"),  updateDBList);
	}
}
