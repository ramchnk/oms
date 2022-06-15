package com.sellinall.lazada.exception;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

public class HandleException implements Processor {
	static Logger log = Logger.getLogger(HandleException.class.getName());

	public void process(Exchange exchange) throws Exception {
		if (exchange.getIn().getHeaders().containsKey("processOrderNotification")) {
			log.error("exception occured while processing order notification message for accountNumber : "
					+ exchange.getProperty("accountNumber") + ", nickNameID : " + exchange.getProperty("nickNameID")
					+ " & exception is : " + exchange.getIn().getHeader("processOrderNotification"));
		} else if (exchange.getIn().getHeaders().containsKey("exceptionMessage")) {
			String msg ="";
			if (exchange.getProperties().containsKey("isImportMissingItem")
					&& exchange.getProperty("isImportMissingItem", boolean.class)) {
				exchange.setProperty("isImportMissingItem", false);
			}
			if (exchange.getProperties().containsKey("parentItemID")) {
				msg = "error occurred while processing referenceID :"+ exchange.getProperty("parentItemID") + " and ";
			}
			log.error(msg +"exception is :"+ exchange.getIn().getHeader("exceptionMessage"));
		} else if (exchange.getProperties().containsKey("failureReason")) {
			String failureReason = exchange.getProperty("failureReason", String.class);
			log.error("Error occured when response voucher getting for accountNumber: "
					+ exchange.getProperty("accountNumber") + ", nickNameId: " + exchange.getProperty("nickNameID")
					+ " and the failureReason is: " + failureReason);
		} else if (exchange.getProperties().containsKey("isRequestFromBatch")
				&& exchange.getProperty("isRequestFromBatch", Boolean.class)) {
			exchange.setProperty("failureReason", "Internal error");
			log.error("error occurred while processing remove SKU from inventory for accountNumber: "
					+ exchange.getProperty("accountNumber") + ", nickNameId: " + exchange.getProperty("nickNameID")
					+ " and exception is :"+ exchange.getIn().getHeader("deleteException"));
		} else if (exchange.getIn().getHeaders().containsKey("bulkPriceException")) {
			exchange.setProperty("failureReason", "Internal error please contact support team");
			log.error("error occurred while processing update price sellerSKU for accountNumber: "
					+ exchange.getProperty("accountNumber") + ", nickNameId: " + exchange.getProperty("nickNameID")
					+ " and exception is :" + exchange.getIn().getHeader("bulkPriceException"));
		}
	}
}
