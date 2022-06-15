package com.sellinall.lazada.response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.sellinall.util.enums.SIAInventoryStatus;

public class ProcessGetDetailsResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessGetDetailsResponse.class.getName());

	public void process(Exchange exchange) throws Exception {
		String responseString = exchange.getIn().getBody(String.class);
		JSONObject response = new JSONObject();
		JSONObject responseObject = new JSONObject();
		exchange.setProperty("isOderResponseAvailable", false);
		if (responseString.startsWith("{")) {
			response = new JSONObject(responseString);
			if (response.has("code") && !response.getString("code").equals("0")) {
				log.error("Error occurred while getting order details for accountNumber: "
						+ exchange.getProperty("accountNumber") + ", nickNameId: " + exchange.getProperty("nickNameID")
						+ ",orderID: " + exchange.getProperty("orderID", String.class) + " and the response is: "
						+ response);
				responseObject.put("status", "failed");
				if (response.has("message")) {
					responseObject.put("failureReason", response.getString("message"));
				}
			} else {
				if (exchange.getProperties().containsKey("isSyncMissingOrder")
						&& exchange.getProperty("isSyncMissingOrder", Boolean.class)) {
					responseObject = new JSONObject(responseString);
					if (responseObject.has("data") && responseObject.get("data") instanceof JSONObject
							&& responseObject.getJSONObject("data").length() > 0) {
						exchange.getOut().setBody(responseObject.getJSONObject("data"));
						exchange.setProperty("isOderResponseAvailable", true);
					} else {
						log.error("Error occurred while getting order details for accountNumber: "
								+ exchange.getProperty("accountNumber") + ", nickNameId: "
								+ exchange.getProperty("nickNameID") + ",orderID: " + exchange.getProperty("orderID")
								+ " and the response is: " + response);
					}
					return;
				}
				responseObject.put("status", "success");
				if (exchange.getProperty("apiName", String.class).equals("/order/items/get")) {
					responseObject.put("getOrderItemDetails", new JSONObject(responseString));
				} else if (exchange.getProperty("apiName", String.class).equals("/order/get")) {
					responseObject.put("getOrderDetails", new JSONObject(responseString));
				} else if (exchange.getProperty("apiName", String.class).equals("/product/item/get")
						&& exchange.getProperty("itemID", String.class) != null) {
					responseObject.put("getItemDetailsByItemId", new JSONObject(responseString));
				} else if (exchange.getProperty("apiName", String.class).equals("/product/item/get")
						&& exchange.getProperty("sellerSKU", String.class) != null) {
					responseObject.put("getItemDetailsBySellerSku", new JSONObject(responseString));
				}
			}
		} else {
			responseObject.put("status", "failed");
			responseObject.put("failureReason", "Unable to get details and response is :" + responseString);
			log.error("Error occurred while getting Details for accountNumber: " + exchange.getProperty("accountNumber")
					+ ", nickNameId: " + exchange.getProperty("nickNameID") + " and the response is: " + response);
		}
		exchange.getOut().setBody(responseObject);
	}
}