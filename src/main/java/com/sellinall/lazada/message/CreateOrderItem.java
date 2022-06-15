package com.sellinall.lazada.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.sellinall.lazada.common.LazadaOrderStatus;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.DateUtil;
import com.sellinall.util.enums.SIAShippingType;

public class CreateOrderItem implements Processor {
	static Logger log = Logger.getLogger(CreateOrderItem.class.getName());

	public void process(Exchange exchange) throws Exception {
		exchange.setProperty("orderItems", createOrderItems(exchange));
	}

	private JSONArray createOrderItems(Exchange exchange) throws JSONException, IOException {
		JSONObject orderItemRawData = exchange.getProperty("order", JSONObject.class);
		String paymentMethod = orderItemRawData.getString("payment_method");
		JSONArray orderDetails = exchange.getProperty("orderDetails", JSONArray.class);
		HashMap<String, String> orderSKUMap = (HashMap<String, String>) exchange.getProperty("orderSKUMap");
		HashMap<String, String> orderWeightMap = (HashMap<String, String>) exchange.getProperty("orderWeightMap");
		String currencyCode = exchange.getProperty("currencyCode", String.class);
		String countryCode = exchange.getProperty("countryCode", String.class);
		long shippingAmount = 0;
		JSONObject orderFromDB = LazadaUtil.getOrderDetails(exchange);
		exchange.setProperty("orderFromDB", orderFromDB);
		Set<BasicDBObject> shippingTrackingDetailsSet = new HashSet<BasicDBObject>();
		Set<String> orderChannelVoucherCodes = new HashSet<String>();
		JSONArray orderItems = new JSONArray();
		List<String> orderItemIDs = new ArrayList<String>();
		Set<String> orderSellerVoucherCodes = new HashSet<String>();
		Set<String> shippingTypes = new HashSet<String>();
		Set<String> orderTypes = new HashSet<String>();
		long totalVoucherSellerSIAFormat = 0;
		long totalVoucherPlatformSIAFormat = 0;
		long totalVoucherSellerLpiSIAFormat = 0;
		double orderPackageWeight = 0;
		for (int i = 0; i < orderDetails.length(); i++) {
			JSONObject orderItem = new JSONObject();
			JSONObject order = orderDetails.getJSONObject(i);
			String voucherAmountStr = order.getString("voucher_amount");
			if (order.has("status") && order.getString("status").equals("canceled")) {
				exchange.setProperty("lazadaCancelReason", order.getString("reason"));
			}
			log.debug("order item raw data:" + orderItemRawData);

			String orderItemID = order.getString("order_item_id");
			orderItem.put("orderItemID", orderItemID);
			orderItemIDs.add(orderItemID);
			orderItem.put("shopId", order.getString("shop_id"));
			/** Set SKU */
			if (orderSKUMap != null) {
				orderItem.put("SKU", orderSKUMap.get(order.getString("sku")));
			}
			/** Set weight */
			if (orderWeightMap != null && orderWeightMap.containsKey(order.getString("sku")) ) {
				String itemWeight = orderWeightMap.get(order.getString("sku"));
				orderPackageWeight += Double.parseDouble(itemWeight);
			}
			/** Set wmsID */
			if (exchange.getProperties().containsKey("wmsID")) {
				orderItem.put("wmsID", exchange.getProperty("wmsID", String.class));
			}
			if (order.has("shipping_amount")) {
				shippingAmount = shippingAmount
						+ CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(order.getString("shipping_amount")));
				orderItem.put("shippingAmount", CurrencyUtil.getJSONAmountObject(
						CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(order.getString("shipping_amount"))),
						currencyCode));
			}

			if (order.has("sku")) {
				String customSKU = order.getString("sku");
				orderItem.put("customSKU", customSKU);
			}
			if (order.has("shop_sku")) {
				orderItem.put("shopSKU", order.getString("shop_sku"));
			}

			/** Set Quantity as number of items sold */
			int orderedQuantity = 1;
			if (orderDetails.length() == 1 && orderItemRawData.getInt("items_count") != 0) {
				// Need To Handle Multiple item in single order
				orderedQuantity = orderItemRawData.getInt("items_count");
			}
			orderItem.put("quantity", orderedQuantity);
			/** Compute the amount if any shipping cost included **/
			String stringItemAmount = order.getString("item_price");
			/** Some times item amount might not exist so check before we set */
			if (!stringItemAmount.isEmpty()) {
				long itemAmtSIAFormat = CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(stringItemAmount));
				orderItem.put("itemAmount",
						CurrencyUtil.getJSONAmountObject(itemAmtSIAFormat / orderedQuantity, currencyCode));
			}
			orderItem.put("itemTitle", order.getString("name"));

			long voucherSellerLpiFormSIAFormat = 0;
			if (order.has("voucher_seller_lpi") && !order.getString("voucher_seller_lpi").isEmpty()) {
				voucherSellerLpiFormSIAFormat = CurrencyUtil
						.convertAmountToSIAFormat(Double.parseDouble(order.getString("voucher_seller_lpi")));
				totalVoucherSellerLpiSIAFormat += voucherSellerLpiFormSIAFormat;
			}
			orderItem.put("sellerBornChannelDiscount",
					CurrencyUtil.getJSONAmountObject(voucherSellerLpiFormSIAFormat, currencyCode));
			String stringVoucherSeller = order.getString("voucher_seller");
			long voucherSellerSIAFormat = 0;
			if (!stringVoucherSeller.isEmpty()) {
				voucherSellerSIAFormat = CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(stringVoucherSeller));
			}
			voucherSellerSIAFormat += voucherSellerLpiFormSIAFormat;
			totalVoucherSellerSIAFormat += voucherSellerSIAFormat;
			orderItem.put("sellerDiscountAmount",
					CurrencyUtil.getJSONAmountObject(voucherSellerSIAFormat, currencyCode));
			String stringVoucherPlatform = order.getString("voucher_platform");
			long voucherPlatformSIAFormat = 0;
			if (!stringVoucherPlatform.isEmpty()) {
				voucherPlatformSIAFormat = CurrencyUtil
						.convertAmountToSIAFormat(Double.parseDouble(stringVoucherPlatform));
			}
			voucherPlatformSIAFormat -= voucherSellerLpiFormSIAFormat;
			totalVoucherPlatformSIAFormat += voucherPlatformSIAFormat;
			orderItem.put("channelDiscountAmount",
					CurrencyUtil.getJSONAmountObject(voucherPlatformSIAFormat, currencyCode));

			/** set Order_id **/
			orderItem.put("orderItemID", order.getString("order_item_id"));
			orderItem.put("siaOrderItemID", order.getString("order_item_id"));

			/** set shipping Type (delivery type) **/
			orderItem.put("deliveryType", order.getString("shipping_type"));
			if (order.has("shipping_provider_type")) {
				String shippingProviderType = order.getString("shipping_provider_type");
				String shippingProviderTypeInSIAFormat = getShippingProviderType(shippingProviderType);
				shippingTypes.add(shippingProviderTypeInSIAFormat);
				orderItem.put("shippingProviderType", shippingProviderType);
			}
			if (order.has("order_type")) {
				orderTypes.add(order.getString("order_type").toUpperCase());
				orderItem.put("orderType", order.getString("order_type").toUpperCase());
			}

			String courierName = "";
			String airwayBill = "";
			String packageID = null;
			if (order.has("tracking_code") && !order.getString("tracking_code").equals("")) {
				courierName = order.getString("shipment_provider");
				airwayBill = order.getString("tracking_code");
				if (order.has("package_id")) {
					packageID = order.getString("package_id");
				}
				boolean isDeliveredBySellerEnabled = false;
				if (courierName.contains("Delivered by Seller")) {
					isDeliveredBySellerEnabled = true;
				}
				exchange.setProperty("isDeliveredBySellerEnabled", isDeliveredBySellerEnabled);
				exchange.setProperty("trackingCode", airwayBill);
				exchange.setProperty("shippingProviderName", courierName);
			}
			orderItem.put("timeOrderCreated", LazadaUtil.getOrderTimeValues(order, "created_at", countryCode));
			orderItem.put("timeOrderUpdated", LazadaUtil.getOrderTimeValues(order, "updated_at", countryCode));
			if (order.has("sla_time_stamp") && !order.getString("sla_time_stamp").isEmpty()) {
				try {
					long shippingDeadLine = LazadaUtil.getOrderTimeValues(order, "sla_time_stamp", countryCode);
					orderItem.put("shippingDeadLine", shippingDeadLine);
					exchange.setProperty("shippingDeadLine", shippingDeadLine);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (order.has("cancel_return_initiator") && !order.getString("cancel_return_initiator").isEmpty()
					&& !order.getString("cancel_return_initiator").equals("null-null")) {
				if (LazadaUtil.lazadaOrderReasonMap.containsKey(order.getString("cancel_return_initiator"))) {
					orderItem.put("cancelledOrReturnedBy",
							LazadaUtil.lazadaOrderReasonMap.get(order.getString("cancel_return_initiator")));
				} else {
					log.warn("cancel_return_initiator : " + order.getString("cancel_return_initiator")
							+ " is not valid value for the orderId: " + exchange.getProperty("orderID")
							+ ", accountNumber : " + exchange.getProperty("accountNumber") + ", nickNameID : "
							+ exchange.getProperty("nickNameID"));
				}
			}
			String status = order.getString("status");
			JSONArray orderStatuses = new JSONArray();
			orderStatuses.put(status);
			LazadaUtil.getStatusDetails(orderStatuses, paymentMethod, orderItem, orderFromDB, exchange, "orderItems");
			if (!airwayBill.isEmpty()) {
				JSONObject shippingTrackingDetails = LazadaUtil.getShippingTrackingDetails(orderFromDB, courierName,
						airwayBill, exchange, order.getString("order_item_id"));
				if (packageID != null && !packageID.isEmpty()) {
					shippingTrackingDetails.put("packageID", packageID);
				}
				orderItem.put("shippingTrackingDetails", shippingTrackingDetails);
				JSONObject shippingTrackingDetailObject = buildShippingTrackingDetailObject(orderFromDB, courierName,
						airwayBill);
				shippingTrackingDetailsSet.add((BasicDBObject) JSON.parse(shippingTrackingDetailObject.toString()));
			} else {
				// maintain old tracking details
				JSONObject oldShippingTrackingDetails = loadOldTrackingDetailsFromDB(exchange, orderFromDB,
						orderItemID);
				if (oldShippingTrackingDetails != null) {
					orderItem.put("shippingTrackingDetails", oldShippingTrackingDetails);
					shippingTrackingDetailsSet.add((BasicDBObject) JSON.parse(oldShippingTrackingDetails.toString()));
				}
			}
			if (order.has("buyer_id") && !order.getString("buyer_id").isEmpty()) {
				exchange.setProperty("buyerID", order.getString("buyer_id"));
			}
			if (!voucherAmountStr.isEmpty() && Float.parseFloat(voucherAmountStr) > 0) {
				Set<String> sellerVoucherCodes = new HashSet<String>();
				Set<String> channelVoucherCodes = new HashSet<String>();
				if (order.has("voucher_code_seller") && !order.getString("voucher_code_seller").isEmpty()
						&& order.has("voucher_seller") && !order.getString("voucher_seller").isEmpty()) {
					sellerVoucherCodes.add(order.getString("voucher_code_seller"));
					orderSellerVoucherCodes.add(order.getString("voucher_code_seller"));
				}
				if (order.has("voucher_code") && !order.getString("voucher_code").isEmpty()
						&& order.has("voucher_platform") && !order.getString("voucher_platform").isEmpty()) {
					channelVoucherCodes.add(order.getString("voucher_code"));
					orderChannelVoucherCodes.add(order.getString("voucher_code"));
				}
				if (!sellerVoucherCodes.isEmpty()) {
				orderItem.put("sellerVoucherCodes",sellerVoucherCodes );
				}
				if (!channelVoucherCodes.isEmpty()) {
				orderItem.put("channelVoucherCodes",channelVoucherCodes);
				}
			}
			Boolean isSellerOwnFleet = false;
			if (order.has("delivery_option_sof") && order.getInt("delivery_option_sof") == 1) {
				isSellerOwnFleet = true;
			}
			orderItem.put("isSellerOwnFleet", isSellerOwnFleet);
			orderItems.put(orderItem);
		}
		// overall voucher codes
		if (!orderSellerVoucherCodes.isEmpty()) {
			exchange.setProperty("sellerVoucherCodes", orderSellerVoucherCodes);
		}
		if (!orderChannelVoucherCodes.isEmpty()) {
			exchange.setProperty("channelVoucherCodes", orderChannelVoucherCodes);
		}
		exchange.setProperty("orderItemIDs", orderItemIDs);
		exchange.setProperty("shippingTypes", shippingTypes);
		exchange.setProperty("orderTypes", orderTypes);
		exchange.setProperty("totalVoucherSellerLpiSIAFormat", totalVoucherSellerLpiSIAFormat);
		exchange.setProperty("totalVoucherSellerSIAFormat", totalVoucherSellerSIAFormat);
		exchange.setProperty("totalVoucherPlatformSIAFormat", totalVoucherPlatformSIAFormat);
		exchange.setProperty("isNewOrder", isNewOrder(orderItemRawData));
		exchange.setProperty("isAlreadyShippingURLExiting", checkExitShippingURL(orderFromDB));
		exchange.setProperty("packageWeight", orderPackageWeight);
		JSONArray shippingTrackingDetailsList = new JSONArray();
		for (BasicDBObject shippingTrackingDetail : shippingTrackingDetailsSet) {
			shippingTrackingDetailsList.put(LazadaUtil.parseToJsonObject((DBObject) shippingTrackingDetail));
		}
		exchange.setProperty("shippingTrackingDetailsList", shippingTrackingDetailsList);
		exchange.setProperty("shippingAmount", shippingAmount);
		return orderItems;
	}

	private boolean checkExitShippingURL(JSONObject orderFromDB) {
		try {
			if (orderFromDB.has("httpCode") && orderFromDB.getInt("httpCode") == 200) {
				JSONObject orderDetails = new JSONObject(orderFromDB.getString("payload"));
				if (orderDetails.has("documents")) {
					JSONObject documents = (JSONObject) orderDetails.get("documents");
					if (documents.has("shippingLabelUrl")) {
						String shippingLabelUrl = documents.getString("shippingLabelUrl");
						if (!shippingLabelUrl.isEmpty()) {
							return true;
						}
					}
				}

			}
		} catch (JSONException e) {
			log.error("Error occured while checking shippingLabelUrl is already exit or not : " + e);
		}
		return false;
	}

	private String getShippingProviderType(String shippingProviderType) {
		switch (shippingProviderType.toUpperCase()) {
		case "ECONOMY":
			return SIAShippingType.ECONOMY.toString();
		case "STANDARD":
			return SIAShippingType.STANDARD.toString();
		case "EXPRESS":
			return SIAShippingType.EXPRESS.toString();
		case "INSTANT":
			return SIAShippingType.INSTANT.toString();
		case "SELLER_OWN_FLEET":
			return SIAShippingType.SELLER_OWN_FLEET.toString();
		case "PICKUP_IN_STORE":
			return SIAShippingType.STORE_PICKUP.toString();
		case "DIGITAL":
			return SIAShippingType.DIGITAL.toString();
		default:
			return "";
		}
	}

	private Boolean isNewOrder(JSONObject orderItemRawData) throws JSONException {
		JSONArray statuses = orderItemRawData.getJSONArray("statuses");
		String notificationStatus = LazadaUtil.getOverallOrderStatus(statuses);
		if (LazadaOrderStatus.PENDING.equalsName(notificationStatus)) {
			return true;
		}
		return false;
	}

	private JSONObject buildShippingTrackingDetailObject(JSONObject orderFromDB, String courierName,
			String airwayBillFromRequest) throws JSONException {
		if (orderFromDB.has("httpCode") && orderFromDB.getInt("httpCode") == 200) {
			JSONObject orderDetails = new JSONObject(orderFromDB.getString("payload"));
			JSONObject shippingDetails = (JSONObject) orderDetails.get("shippingDetails");
			if (shippingDetails.has("shippingTrackingDetailsList")) {
				JSONArray shippingTrackingDetailsList = (JSONArray) shippingDetails.get("shippingTrackingDetailsList");
				for (int i = 0; i < shippingTrackingDetailsList.length(); i++) {
					JSONObject shippingTrackingDetail = shippingTrackingDetailsList.getJSONObject(i);
					if (shippingTrackingDetail.has("courierName")
							&& shippingTrackingDetail.getString("courierName").equals(courierName)) {
						String airwayBillFromDB = shippingTrackingDetail.has("airwayBill")
								? shippingTrackingDetail.getString("airwayBill")
								: "";
						if (airwayBillFromDB.equals("") || airwayBillFromDB.equals(airwayBillFromRequest)) {
							shippingTrackingDetail.put("airwayBill", airwayBillFromRequest);
							return shippingTrackingDetail;
						}
					}
				}
			}
		}
		JSONObject shippingTrackingDetail = new JSONObject();
		shippingTrackingDetail.put("courierName", courierName);
		shippingTrackingDetail.put("airwayBill", airwayBillFromRequest);
		return shippingTrackingDetail;
	}

	public static JSONObject loadOldTrackingDetailsFromDB(Exchange exchange, JSONObject orderFromDB,
			String orderItemID) {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		try {
			if (orderFromDB.has("httpCode") && orderFromDB.getInt("httpCode") == 200) {
				JSONObject orderDetails = new JSONObject(orderFromDB.getString("payload"));
				JSONArray orderItems = orderDetails.getJSONArray("orderItems");
				for (int i = 0; i < orderItems.length(); i++) {
					JSONObject orderItem = orderItems.getJSONObject(i);
					String dbOrderItemId = orderItem.getString("orderItemID");
					if (orderItemID.equals(dbOrderItemId)) {
						if (orderItem.has("shippingTrackingDetails")) {
							JSONObject shippingTrackingDetailsFromDB = (JSONObject) orderItem
									.get("shippingTrackingDetails");
							return shippingTrackingDetailsFromDB;
						}
					}
				}
			}
		} catch (JSONException e) {
			log.error("Error occured while getting old tracking details  for account number: " + accountNumber, e);
		}
		return null;
	}
}