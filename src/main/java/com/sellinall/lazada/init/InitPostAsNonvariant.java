package com.sellinall.lazada.init;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;

public class InitPostAsNonvariant implements Processor {
	static Logger log = Logger.getLogger(InitPostAsNonvariant.class.getName());
	
	public void process(Exchange exchange) throws Exception {
		BasicDBObject inventory = exchange.getIn().getBody(BasicDBObject.class);
		ArrayList<BasicDBObject> list =new ArrayList<BasicDBObject>();
		list.add(inventory);
		exchange.setProperty("inventoryDetails", list);
	}
}
