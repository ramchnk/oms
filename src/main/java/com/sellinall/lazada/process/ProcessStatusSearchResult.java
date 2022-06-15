package com.sellinall.lazada.process;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class ProcessStatusSearchResult implements Processor {

	public void process(Exchange exchange) throws Exception {
		List<DBObject> queryResults = (List<DBObject>) exchange.getIn().getBody();
		if (queryResults.isEmpty()) {
			exchange.setProperty("isInventoryResultEmpty", true);
			return;
		}
		ArrayList<String> SKUList = new ArrayList<String>();
		List<BasicDBObject> skuAndRefrenceIdList = new ArrayList<BasicDBObject>();
		for (DBObject inventory : queryResults) {
			BasicDBObject skuAndRefrenceId = new BasicDBObject();
			SKUList.add((String)inventory.get("SKU"));
			BasicDBList channelList = (BasicDBList) inventory.get("lazada");
			BasicDBObject channel = (BasicDBObject) channelList.get(0);
			if (channel.containsField("refrenceID") ) {
				skuAndRefrenceId.put("refrenceID", channel.getString("refrenceID"));
				skuAndRefrenceId.put("SKU", (String)inventory.get("SKU"));
				skuAndRefrenceIdList.add(skuAndRefrenceId);
			}
		}
		exchange.setProperty("SKUList", SKUList);
		exchange.setProperty("isInventoryResultEmpty", false);
		exchange.getOut().setBody(skuAndRefrenceIdList);
	}

}
