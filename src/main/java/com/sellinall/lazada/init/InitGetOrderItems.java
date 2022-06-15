package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;

import com.sellinall.lazada.bl.GetOrderDetails;

public class InitGetOrderItems implements Processor {

	static Logger log = Logger.getLogger(InitGetOrderItems.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		String hostURL = exchange.getProperty("hostURL", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameId = exchange.getProperty("nickNameID", String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String orderID = exchange.getProperty("orderID", String.class);

		exchange.setProperty("responseSuccess", false);
		try {
			JSONArray response = GetOrderDetails.getOrderItems(hostURL, accountNumber, nickNameId, accessToken, orderID,
					1);
			if (response.length() > 0) {
				exchange.setProperty("orderItemResponse", response);
				exchange.getOut().setBody(response);
				exchange.setProperty("responseSuccess", true);
			} else {
				log.error("Failed to process orderID : " + orderID + " for accountNumber : " + accountNumber
						+ ", nickNameId : " + nickNameId);
			}
		} catch (Exception e) {
			log.error("Exception occured while processing orderID : " + orderID + " for accountNumber : "
					+ accountNumber + ", nickNameId : " + nickNameId);
			e.printStackTrace();
		}
	}

}
