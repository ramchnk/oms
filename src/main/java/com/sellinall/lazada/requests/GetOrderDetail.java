package com.sellinall.lazada.requests;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.bl.GetOrderDetails;
import com.sellinall.lazada.util.LazadaUtil;

public class GetOrderDetail implements Processor {

	static Logger log = Logger.getLogger(GetOrderDetail.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		String hostURL = exchange.getProperty("hostURL", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameId = exchange.getProperty("nickNameID", String.class);
		String countryCode = exchange.getProperty("countryCode", String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String orderID = exchange.getProperty("orderID", String.class);

		exchange.setProperty("responseSuccess", false);
		try {
			JSONObject order = GetOrderDetails.getOrder(hostURL, accountNumber, nickNameId, accessToken, orderID, 1);
			if (order.length() > 0) {
				exchange.setProperty("order", order);
				exchange.setProperty("responseSuccess", true);
				exchange.setProperty("timeOrderCreated", LazadaUtil.getOrderTimeValues(order, "created_at", countryCode));
				exchange.setProperty("timeOrderUpdated", LazadaUtil.getOrderTimeValues(order, "updated_at", countryCode));
				checkIsEligibleToProcessOrder(exchange, order);
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

	private void checkIsEligibleToProcessOrder(Exchange exchange, JSONObject order) throws IOException, JSONException {
		if (order.has("statuses") && order.getJSONArray("statuses").length() == 1
				&& order.getJSONArray("statuses").getString(0).equalsIgnoreCase("pending")) {
			boolean isEligibleToProcessOrder = LazadaUtil.checkIsEligibleToProcessOrder(exchange, order);
			if (!isEligibleToProcessOrder) {
				String accountNumber = exchange.getProperty("accountNumber", String.class);
				String nickNameId = exchange.getProperty("nickNameID", String.class);
				String orderID = exchange.getProperty("orderID", String.class);

				// New order notification coming & it had already package id in DB, so skipping
				// this notification msg
				log.error("skipping duplicate notification for orderID : " + orderID + ", accountNumber : "
						+ accountNumber + ", nickNameID : " + nickNameId);
				exchange.setProperty("responseSuccess", false);
			}
		}
	}

}
