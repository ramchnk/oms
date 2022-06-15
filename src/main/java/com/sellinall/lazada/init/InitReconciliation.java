/**
 * 
 */
package com.sellinall.lazada.init;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author vikraman
 * 
 */
public class InitReconciliation implements Processor {
	static Logger log = Logger.getLogger(InitReconciliation.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		log.info("Inbody " + inBody.toString());
		exchange.setProperty("accountNumber", inBody.getString("accountNumber"));
		String nickNameId = inBody.getString("nickNameID");
		exchange.setProperty("nickNameID", nickNameId);
		exchange.setProperty("channelName", nickNameId.split("-")[0]);
		exchange.setProperty("reconciliationFileURL", inBody.getString("reconciliationFileURL"));
		exchange.setProperty("reconciliationRecordObjectId", inBody.getString("reconciliationRecordObjectId"));
		exchange.setProperty("orderIndex", 1);
		exchange.setProperty("isLastRow", false);
		exchange.setProperty("noOfOrders", 0);
		exchange.setProperty("noOfOrdersProcessed", 0);
		exchange.setProperty("noOfOrdersSkipped", 0);
		exchange.setProperty("receivedAmount", 0);
		exchange.setProperty("awaitingAmount", 0);
		// Will handle later (1/30/2017)
		exchange.setProperty("totalAmount", 0);
		exchange.setProperty("refundedAmount", 0);
		exchange.setProperty("processedOrders", new ArrayList<String>());
		exchange.setProperty("skippedOrders", new ArrayList<String>());
		exchange.setProperty("totalOtherFee", 0);
		exchange.setProperty("totalSellerCreditAmount", 0);
		exchange.setProperty("noOfRowsInOtherFee", 0);
		exchange.setProperty("noOfRowsInSellerCredit", 0);
		exchange.setProperty("timeSettlementProcessed", System.currentTimeMillis()/1000);
		if (inBody.has("isAdmin")) {
			exchange.setProperty("isAdmin", inBody.getBoolean("isAdmin"));
		}
		boolean settlementAmtMatchedBankTransfer = false;
		if (inBody.has("settlementAmtMatchedBankTransfer")) {
			settlementAmtMatchedBankTransfer = inBody.getBoolean("settlementAmtMatchedBankTransfer");
		}
		exchange.setProperty("settlementAmtMatchedBankTransfer", settlementAmtMatchedBankTransfer);
	}
}