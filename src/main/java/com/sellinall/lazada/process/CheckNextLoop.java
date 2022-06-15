package com.sellinall.lazada.process;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mudra.sellinall.config.Config;

public class CheckNextLoop implements Processor {
	static Logger log = Logger.getLogger(CheckNextLoop.class.getName());

	public void process(Exchange exchange) throws Exception {
		int pageNumber = exchange.getProperty("pageNumber", Integer.class);
		int totalVoucher = 0;
		if (exchange.getProperties().containsKey("totalVoucher")) {
			totalVoucher = exchange.getProperty("totalVoucher", Integer.class);
		}
		int page_size = Config.getConfig().getRecordsPerPage();
		int noOfPage = (totalVoucher / page_size) + (totalVoucher % page_size > 0 ? 1 : 0);
		if (noOfPage <= pageNumber) {
			exchange.setProperty("isLastPage", true);
		} else {
			exchange.setProperty("pageNumber", pageNumber + 1);
		}
	}

}
