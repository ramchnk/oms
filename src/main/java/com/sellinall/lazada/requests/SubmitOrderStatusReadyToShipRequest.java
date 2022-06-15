package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.enums.OrderUpdateStatus;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.SIAShippingStatus;

public class SubmitOrderStatusReadyToShipRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitOrderStatusReadyToShipRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		String response = "";
		String accessToken = exchange.getProperty("accessToken", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		ArrayList<String> orderItemIDs = (ArrayList<String>) exchange.getProperty("orderItemIDs");

		boolean orderHasMultiplePackages = false;
		if (exchange.getProperties().containsKey("orderHasMultiplePackages")) {
			orderHasMultiplePackages = exchange.getProperty("orderHasMultiplePackages", Boolean.class);
		}
		String airwayBill = "", courierName = "";
		if (exchange.getProperties().containsKey("airwayBill")) {
			airwayBill = exchange.getProperty("airwayBill", String.class);
		}
		if (exchange.getProperties().containsKey("courierName")) {
			courierName = exchange.getProperty("courierName", String.class);
		}
		if (orderHasMultiplePackages) {
			int index = exchange.getProperty("CamelLoopIndex", Integer.class);
			airwayBill = (String) exchange.getProperty("airwayBillNumbers", List.class).get(index);
			Map<String, BasicDBObject> orderItemsMap = exchange.getProperty("orderItemsMap", Map.class);
			BasicDBObject orderItemDetails = orderItemsMap.get(airwayBill);
			courierName = orderItemDetails.getString("courierName");
			orderItemIDs = (ArrayList<String>) orderItemDetails.get("orderItemIDs");
		}

		JSONObject order = new JSONObject();
		String orderID = "";
		if (exchange.getProperties().containsKey("order")) {
			order = exchange.getProperty("order", JSONObject.class);
			orderID = order.getString("orderID");
		}
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("order_item_ids", orderItemIDs.toString());
		map.put("access_token", accessToken);
		map.put("delivery_type", "dropship");
		String queryParams = "&delivery_type=dropship";

		if (!courierName.isEmpty()) {
			map.put("shipment_provider", courierName);
			queryParams += "&shipment_provider=" + URLEncoder.encode(courierName, "UTF-8");
		}
		queryParams += "&order_item_ids=" + URLEncoder.encode(orderItemIDs.toString(), "UTF-8");
		JSONObject serviceResponse = new JSONObject();
		try {
			if (!airwayBill.isEmpty()) {
				map.put("tracking_number", airwayBill);
				queryParams += "&tracking_number=" + airwayBill;
			}

			String clientID = Config.getConfig().getLazadaClientID();
			String clientSecret = Config.getConfig().getLazadaClientSecret();
			response = NewLazadaConnectionUtil.callAPI(hostURL, "/order/rts", accessToken, map, "", queryParams,
					"POST", clientID, clientSecret);
			 serviceResponse = new JSONObject(response);
			if (serviceResponse.has("code") && serviceResponse.getString("code").equals("0")) {
				log.info("response from SubmitOrderStatusReadyToShipRequest: " + serviceResponse);
				exchange.setProperty("updateStatus", OrderUpdateStatus.COMPLETE.toString());
				List<String> updatedOrderItems = exchange.getProperty("updatedOrderItems", List.class);
				updatedOrderItems.addAll(orderItemIDs);
				return;
			}
			log.error("Order Update failed for orderID: " + orderID + ", accountNumber: " + accountNumber
					+ ",nickNameID " + nickNameID + ", request:" + queryParams + " and response:" + response);
			updateFailedOrderItemIDs(exchange, orderItemIDs);
			exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
			if(serviceResponse.has("message")) {
				exchange.setProperty("failureReason", serviceResponse.getString("message"));
			}
		} catch (Exception e) {
			log.error("Order Update failed for orderID: " + orderID + ", accountNumber: " + accountNumber
					+ ",nickNameID " + nickNameID + ", request:" + queryParams + " and response:" + response);
			e.printStackTrace();
			updateFailedOrderItemIDs(exchange, orderItemIDs);
			exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
			if(serviceResponse.has("message")) {
				exchange.setProperty("failureReason", serviceResponse.getString("message"));
			}
		}
		// To revert the currentStatus of order from PROCESSING to any when
		// order update failed for some reasons.
		if ((exchange.getProperties().containsKey("toState")
				&& exchange.getProperty("toState", String.class).equals(SIAShippingStatus.READY_TO_SHIP.toString())
				&& order.has("orderStatus")
				&& order.getString("orderStatus").equals(SIAOrderStatus.PROCESSING.toString())) || orderHasMultiplePackages) {
			LazadaUtil.setCurrentOrderStatus(exchange, order, orderItemIDs, orderHasMultiplePackages);
		}

	}

	public static void updateFailedOrderItemIDs(Exchange exchange, ArrayList<String> orderItemIDs) {
		List<String> updateFailedOrderItems = exchange.getProperty("updateFailedOrderItems", List.class);
		updateFailedOrderItems.addAll(orderItemIDs);
	}

}