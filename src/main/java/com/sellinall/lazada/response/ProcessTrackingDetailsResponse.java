package com.sellinall.lazada.response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.util.enums.OrderUpdateStatus;
import com.sellinall.util.enums.SIAShippingStatus;

public class ProcessTrackingDetailsResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessTrackingDetailsResponse.class.getName());

	public void process(Exchange exchange) throws Exception {
		String body = exchange.getIn().getBody(String.class);
		JSONObject serviceResponse = new JSONObject();
		JSONObject order = exchange.getProperty("order", JSONObject.class);
		if (body.startsWith("{")) {
			serviceResponse = new JSONObject(body);
		} else {
			log.error(exchange.getProperty("courierName", String.class) + " : Logistic server error for orderID : "
					+ order.getString("orderID") + ", reason : " + body);
			exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
			return;
		}
		if (serviceResponse.has("shippingTrackingDetails")) {
			JSONObject shippingTrackingDeatils = order.getJSONObject("shippingDetails")
					.getJSONObject("shippingTrackingDetails");
			JSONObject trackingDetailsResponse = serviceResponse.getJSONObject("shippingTrackingDetails");
			shippingTrackingDeatils.put("airwayBill", trackingDetailsResponse.getString("airwayBill"));
			exchange.setProperty("airwayBill", trackingDetailsResponse.getString("airwayBill"));
			shippingTrackingDeatils.put("courierName", trackingDetailsResponse.getString("courierName"));
			shippingTrackingDeatils.put("trackingURL", trackingDetailsResponse.getString("trackingURL"));
			if (trackingDetailsResponse.has("documentURL")) {
				shippingTrackingDeatils.put("documentURL", trackingDetailsResponse.getString("documentURL"));
			}
			if(serviceResponse.has("shippingLabelUrl")) {
				JSONObject documents = new JSONObject();
				documents.put("shippingLabelUrl", serviceResponse.getString("shippingLabelUrl"));
				order.put("documents", documents);
			}
			if (serviceResponse.has("documents")) {
				JSONObject documentsFromAPIResponse = serviceResponse.getJSONObject("documents");
				exchange.setProperty("invoiceUrl", documentsFromAPIResponse.getString("invoiceUrl"));
				exchange.setProperty("shippingLabelUrl", documentsFromAPIResponse.getString("shippingLabelUrl"));
			}
			order.put("shippingCarrierStatus", SIAShippingStatus.NOT_SHIPPED.toString());
			exchange.setProperty("order", order);
			exchange.setProperty("updateStatus", OrderUpdateStatus.COMPLETE.toString());
		} else {
			log.error("orderCreation in " + exchange.getProperty("courierName", String.class)
					+ " is failed for orderID : " + order.getString("orderID") + ", reason : " + serviceResponse);
			exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
			if(serviceResponse.has("failureReason")) {
				exchange.setProperty("failureReason", serviceResponse.getString("failureReason"));
			}
		}
	}
}
