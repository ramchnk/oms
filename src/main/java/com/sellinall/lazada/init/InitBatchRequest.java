package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

public class InitBatchRequest implements Processor {
	static Logger log = Logger.getLogger(InitBatchRequest.class.getName());
	
	public void process(Exchange exchange) throws Exception {
		exchange.getOut().setBody(exchange.getProperty("inputRequest"));
	}
}
