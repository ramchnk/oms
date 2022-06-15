package com.sellinall.lazada.message;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.lazada.common.LazadaOrderStatus;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.CurrencyUtil;

public class ConstructMessageForPNQ implements Processor {
	static Logger log = Logger.getLogger(ConstructMessageForPNQ.class.getName());

	public void process(Exchange exchange) throws Exception {
		exchange.getOut().setBody(createMessage(exchange));
	}

	private String getNicknameId(DBObject userDetails, String channelName) {
		BasicDBObject channel = (BasicDBObject) userDetails.get(channelName);
		BasicDBObject nickName = (BasicDBObject) channel.get("nickName");
		return nickName.getString("id");
	}

	private JSONObject createMessage(Exchange exchange) throws JSONException, IOException {
		JSONArray orderItems = exchange.getProperty("orderItems", JSONArray.class);
		JSONObject orderDetails = (JSONObject) exchange.getProperty("order");

		JSONObject orderNotificationMessage = new JSONObject();

		log.debug(orderDetails);
		/** Set nickname id */
		orderNotificationMessage.put("nickNameID",
				getNicknameId((DBObject) exchange.getProperty("UserDetails"), "lazada"));
		orderNotificationMessage.put("accountNumber", exchange.getProperty("accountNumber"));

		String paymentMethod = orderDetails.getString("payment_method");

		if (exchange.getProperties().containsKey("sellerVoucherCodes")) {
			orderNotificationMessage.put("sellerVoucherCodes", exchange.getProperty("sellerVoucherCodes", Set.class));
		}
		if (exchange.getProperties().containsKey("channelVoucherCodes")) {
			orderNotificationMessage.put("channelVoucherCodes", exchange.getProperty("channelVoucherCodes", Set.class));
		}

		JSONArray statuses = orderDetails.getJSONArray("statuses");
		JSONObject orderFromDB = new JSONObject();
		if (exchange.getProperties().containsKey("orderFromDB")) {
			orderFromDB = (JSONObject) exchange.getProperty("orderFromDB");
		} else {
			orderFromDB = LazadaUtil.getOrderDetails(exchange);
			exchange.setProperty("orderFromDB", orderFromDB);
		}
		LazadaUtil.getStatusDetails(statuses, paymentMethod, orderNotificationMessage, orderFromDB, exchange, "order");
		//TODO: need to be handled multiple notificationStatus for Auto-Accept
		String notificationStatus = statuses.getString(0);
		log.debug("notification Status " + notificationStatus + "   " + LazadaOrderStatus.SHIPPED.name());
		exchange.setProperty("notificationStatus", notificationStatus);
		String stringOrderAmount = orderDetails.getString("price");
		String orderCurrencyCode = exchange.getProperty("currencyCode", String.class);

		/**
		 * Some times order total amount might not exist so check before we set
		 */
		if (!stringOrderAmount.isEmpty()) {
			long shippingAmtSIAFormat = CurrencyUtil
					.convertAmountToSIAFormat(Double.parseDouble(stringOrderAmount.replaceAll(",", "")));
			/** Set Shipping Amount object with amount and currency **/
			orderNotificationMessage.put("orderAmount",
					CurrencyUtil.getJSONAmountObject(shippingAmtSIAFormat, orderCurrencyCode));
		}

		/** set Order_id **/
		String orderID = orderDetails.get("order_id").toString();
		orderNotificationMessage.put("orderID", orderID);
		exchange.setProperty("orderID", orderID);
		/** set Order Number **/
		orderNotificationMessage.put("orderNumber", orderDetails.get("order_number").toString());

		/** Set shipping details */
		JSONObject shippingDetails = buildShippingDetails(exchange,
				orderDetails.getJSONObject("address_shipping"));
		if (shippingDetails != null) {
			orderNotificationMessage.put("shippingDetails", shippingDetails);
		}

		/** set buyer details **/
		String buyerID = "";
		if (exchange.getProperties().containsKey("buyerID")) {
			buyerID = exchange.getProperty("buyerID", String.class);
		}
		JSONObject buyerDetails = buildBuyerDetails(orderDetails, buyerID);
		if (buyerDetails != null) {
			orderNotificationMessage.put("buyerDetails", buyerDetails);
		}

		/** Set Payment method */
		/**
		 * TODO: fix the parser for payment method as JP and CN can have
		 * multiple payment methods
		 */

		orderNotificationMessage.put("paymentMethods", new JSONArray().put(paymentMethod));

		/** Set Site information */
		orderNotificationMessage.put("site", "lazada");

		orderNotificationMessage.put("orderItems", orderItems);
		if (exchange.getProperties().containsKey("orderTypes")) {
			Set<String> orderTypes = (Set<String>) exchange.getProperty("orderTypes");
			orderNotificationMessage.put("orderTypes", orderTypes);
		}
		if (exchange.getProperties().containsKey("shippingTypes")) {
			Set<String> shippingTypes = (Set<String>) exchange.getProperty("shippingTypes");
			orderNotificationMessage.put("shippingTypes", shippingTypes);
		}
		String currencyCode = exchange.getProperty("currencyCode", String.class);

		long shippingAmount = exchange.getProperty("shippingAmount", Long.class);
		orderNotificationMessage.put("shippingAmount", CurrencyUtil.getJSONAmountObject(shippingAmount, currencyCode));
		if (orderDetails.has("voucher_seller")) {
			String stringSellerVoucher = orderDetails.getString("voucher_seller");
			long sellerVoucher = 0;
			if (!stringSellerVoucher.isEmpty()) {
				sellerVoucher = CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(stringSellerVoucher));
			}
			sellerVoucher += exchange.getProperty("totalVoucherSellerLpiSIAFormat", Long.class);
			orderNotificationMessage.put("sellerDiscountAmount",
					CurrencyUtil.getJSONAmountObject(sellerVoucher, currencyCode));
		} else {
			long totalVoucherSellerSIAFormat = exchange.getProperty("totalVoucherSellerSIAFormat", Long.class);
			orderNotificationMessage.put("sellerDiscountAmount",
					CurrencyUtil.getJSONAmountObject(totalVoucherSellerSIAFormat, currencyCode));
		}
		if (orderDetails.has("voucher_platform")) {
			String stringChannelVoucher = orderDetails.getString("voucher_platform");
			long channelVoucher = 0;
			if (!stringChannelVoucher.isEmpty()) {
				channelVoucher = CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(stringChannelVoucher));
			}
			channelVoucher -= exchange.getProperty("totalVoucherSellerLpiSIAFormat", Long.class);
			orderNotificationMessage.put("channelDiscountAmount",
					CurrencyUtil.getJSONAmountObject(channelVoucher, currencyCode));
		} else {
			long totalVoucherPlatformSIAFormat = exchange.getProperty("totalVoucherPlatformSIAFormat", Long.class);
			orderNotificationMessage.put("channelDiscountAmount",
					CurrencyUtil.getJSONAmountObject(totalVoucherPlatformSIAFormat, currencyCode));
		}
		if(orderDetails.has("documents")) {
			orderNotificationMessage.put("documents", orderDetails.getJSONObject("documents"));
		}
		orderNotificationMessage.put("timeOrderCreated", exchange.getProperty("timeOrderCreated", Long.class));
		orderNotificationMessage.put("timeOrderUpdated", exchange.getProperty("timeOrderUpdated", Long.class));
		orderNotificationMessage.put("addendum", LazadaUtil.buildOrderAddendumObj(false));
		if (exchange.getProperty("packageWeight", Double.class) != null) {
			orderNotificationMessage.put("packageWeight", exchange.getProperty("packageWeight", Double.class));
		}
		log.debug("Order Message = " + orderNotificationMessage.toString());
		exchange.setProperty("order", orderNotificationMessage);
		return orderNotificationMessage;
	}

	private JSONObject buildBuyerDetails(JSONObject orderDetails, String buyerID) throws JSONException {
		JSONObject buyerDetails = new JSONObject();
		buyerDetails.put("name", orderDetails.getString("customer_first_name"));
		if (!buyerID.isEmpty()) {
			buyerDetails.put("buyerID", buyerID);
		}
		return buyerDetails;
	}

	private JSONObject buildShippingDetails(Exchange exchange, JSONObject shipping) throws JSONException, IOException {
		JSONObject shippingDetails = new JSONObject();

		/** set shipping address */
		JSONObject address = buildShippingAddress(shipping);
		if (address != null) {
			shippingDetails.put("address", address);
		}

		if (shippingDetails.length() == 0) {
			return null;
		}
		if (exchange.getProperties().containsKey("trackingCode")) {
			String courierName = exchange.getProperty("shippingProviderName", String.class);
			String airwayBill = exchange.getProperty("trackingCode", String.class);
			JSONObject orderFromDB = (JSONObject) exchange.getProperty("orderFromDB");
			JSONObject shippingTrackingDetails = LazadaUtil.getShippingTrackingDetails(orderFromDB, courierName,
					airwayBill, exchange, null);
			shippingDetails.put("shippingTrackingDetails", shippingTrackingDetails);
		} else {
			// maintain old tracking details 
			JSONObject orderFromDB = (JSONObject) exchange.getProperty("orderFromDB");
			JSONObject oldShippingTrackingDetails = loadOldTrackingDetailsFromDB(exchange, orderFromDB);
			if (oldShippingTrackingDetails != null) {
				shippingDetails.put("shippingTrackingDetails", oldShippingTrackingDetails);
			}
		}
		if (exchange.getProperties().containsKey("shippingTrackingDetailsList")) {
			JSONArray shippingTrackingDetailsList = exchange.getProperty("shippingTrackingDetailsList", JSONArray.class);
			if(shippingTrackingDetailsList.length() !=0) {
				shippingDetails.put("shippingTrackingDetailsList", shippingTrackingDetailsList);
			}
		}
		if(exchange.getProperty("shippingDeadLine") != null) {
			shippingDetails.put("shippingDeadLine", exchange.getProperty("shippingDeadLine", Long.class));
		}
		return shippingDetails;
	}

	private JSONObject buildShippingAddress(JSONObject shipingAddress) throws JSONException {
		JSONObject buyerAddress = new JSONObject();
		String name = shipingAddress.getString("first_name");
		String street1 = shipingAddress.getString("address1");
		String street2 = shipingAddress.getString("address2");
		String city = shipingAddress.getString("city");
		String country = shipingAddress.getString("country");
		String postalCode = shipingAddress.getString("post_code");
		String phone = shipingAddress.getString("phone");
		buyerAddress.put("name", name);
		buyerAddress.put("street1", street1);
		buyerAddress.put("street2", street2);
		//address3 contains state name
		if (shipingAddress.has("address3")) {
		    String state = shipingAddress.getString("address3");
		    buyerAddress.put("state", state);
		}
		buyerAddress.put("city", city);
		buyerAddress.put("country", country);
		buyerAddress.put("phone", phone);
		if (postalCode.matches("[0-9]+")) {
			buyerAddress.put("postalCode", postalCode);
		}
		return buyerAddress;
	}

	private JSONObject loadOldTrackingDetailsFromDB(Exchange exchange, JSONObject orderFromDB) {
		String nickNameID = getNicknameId((DBObject) exchange.getProperty("UserDetails"), "lazada");
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		try {
			if (orderFromDB.has("httpCode") && orderFromDB.getInt("httpCode") == 200) {
				JSONObject orderDetails = new JSONObject(orderFromDB.getString("payload"));
				JSONObject shippingDetailsFromDB = (JSONObject) orderDetails.get("shippingDetails");
				if (shippingDetailsFromDB.has("shippingTrackingDetails")) {
					JSONObject shippingTrackingDetailsFromDB = (JSONObject) shippingDetailsFromDB
							.get("shippingTrackingDetails");
					return shippingTrackingDetailsFromDB;
				}
			}
		} catch (JSONException e) {
			log.error("Error occured while getting old tracking details  for account number: " + accountNumber
					+ ", nickNameID: " + nickNameID, e);
		}
		return null;
	}

}