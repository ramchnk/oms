package com.sellinall.lazada.db;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class IncrementInvRecordsCreated implements Processor {

	public void process(Exchange exchange) throws Exception {
		int invRecordsCreated = exchange.getProperty("invRecordsCreated", Integer.class);
		invRecordsCreated += exchange.getIn().getHeader("CamelMongoOid", List.class).size();
		exchange.setProperty("invRecordsCreated", invRecordsCreated);
	}
}