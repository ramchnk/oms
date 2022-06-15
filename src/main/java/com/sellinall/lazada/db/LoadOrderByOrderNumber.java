/**
 * 
 */
package com.sellinall.lazada.db;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mudra.sellinall.database.DbUtilities;

/**
 * @author Ramchdanran.K
 * 
 */
public class LoadOrderByOrderNumber implements Processor {
	static Logger log = Logger.getLogger(LoadOrderByOrderNumber.class.getName());

	public void process(Exchange exchange) throws Exception {
		BasicDBObject orderData = exchange.getIn().getBody(BasicDBObject.class);
		exchange.setProperty("order", orderData);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String orderNumber = orderData.getString("orderNumber");
		String nickNameId = exchange.getProperty("nickNameID", String.class);
		BasicDBObject orderFromDB = loadOrderByOrderNumber(orderNumber, accountNumber, nickNameId);
		if (orderFromDB == null) {
			exchange.setProperty("isOrderExist", false);
			processSkippedOrders(exchange, orderNumber);
			return;
		}
		exchange.setProperty("isOrderExist", true);
		processProcessedOrders(exchange, orderNumber);
		exchange.getOut().setBody(orderFromDB);
	}

	private BasicDBObject loadOrderByOrderNumber(String orderNumber, String accountNumber, String nickNameId) {
		DBCollection table = DbUtilities.getOrderDBCollection("order");
		BasicDBObject searchQuery = new BasicDBObject();
		if (orderNumber.equals("")) {
			return null;
		}
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("orderNumber", orderNumber);
		searchQuery.put("site.nickNameID", nickNameId);
		log.debug(searchQuery.toString());
		return (BasicDBObject) table.findOne(searchQuery);
	}

	private void processSkippedOrders(Exchange exchange, String orderNumber) {
			//No need to process auto reconcile		
			ArrayList<String> skippedOrders = exchange.getProperty("skippedOrders", ArrayList.class);
			skippedOrders.add(orderNumber);
	}
	
	private void processProcessedOrders(Exchange exchange, String orderNumber) {
			//No need to process auto reconcile		
			ArrayList<String> processedOrders = exchange.getProperty("processedOrders", ArrayList.class);
			processedOrders.add(orderNumber);		
	}
}