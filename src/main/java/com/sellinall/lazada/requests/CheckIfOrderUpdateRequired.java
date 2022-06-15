package com.sellinall.lazada.requests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.APIUrlConfig;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.SIAShippingStatus;

public class CheckIfOrderUpdateRequired implements Processor {
	static Logger log = Logger.getLogger(CheckIfOrderUpdateRequired.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject order = (JSONObject) exchange.getProperty("order");
		boolean updatePaymentStatus = order.getBoolean("needToUpdatePayment");
		boolean updateFeedback = order.getBoolean("needToUpdateFeedBack");
		boolean updateShippingDetails = order.getBoolean("needToUpdateShipping");
		boolean isPartialProcessing = isOrderPartiallyProcessing(order);
		boolean isRepackOrder = false;
		if (isPartialProcessing) {
			isRepackOrder = isRepackOrder(order, exchange);
		}
		exchange.setProperty("isRepackOrder", isRepackOrder);
		exchange.setProperty("isPartialProcessing", isPartialProcessing);
		exchange.setProperty("isProcessingDelivered", false);
		SIAOrderStatus orderStatus = SIAOrderStatus.valueOf(order.getString("orderStatus"));
		JSONArray orderItems = order.getJSONArray("orderItems");
		if (order.getBoolean("needToUpdateOrder") || order.getBoolean("needToUpdateShipping")) {
			SIAShippingStatus shippingStatus = SIAShippingStatus.valueOf(order.getString("shippingStatus"));
			ArrayList<String> orderItemIDs = getOrderItemId(order, exchange);
			exchange.setProperty("orderItemIDs", orderItemIDs);
			if (isPartialProcessing) {
				orderStatus = SIAOrderStatus.valueOf(getPartialShippingStatus(orderItems, orderItemIDs, "orderStatus"));
				if (orderStatus != SIAOrderStatus.CANCELLED) {
					shippingStatus = SIAShippingStatus
							.valueOf(getPartialShippingStatus(orderItems, orderItemIDs, "shippingStatus"));
				}
			}
			if (exchange.getProperty("isAutoPackOrder", Boolean.class)) {
				exchange.getOut().setHeader("orderUpdateStatus", SIAShippingStatus.READY_TO_SHIP.toString());
				processShippingDetails(exchange, order);
			} else if (orderStatus == SIAOrderStatus.ACCEPTED) {
				exchange.getOut().setHeader("orderUpdateStatus", SIAShippingStatus.READY_TO_SHIP.toString());
				if (isPartialProcessing) {
					// Partial processing enabled only for Lazada Integrated
					// Logistics
					exchange.setProperty("siaIntegratedShippingProvider", false);
					processShippingDetailsForPartialProcessing(exchange, order);
				} else {
					updateStatusToProcessing(order);
					processShippingDetails(exchange, order);
				}
			} else if (order.has("shippingCarrierStatus") && SIAShippingStatus
					.valueOf(order.getString("shippingCarrierStatus")) == SIAShippingStatus.DELIVERED) {
				// To mark as delivered in SOF portal
				exchange.setProperty("isProcessingDelivered", true);
				order.put("needToUpdateOrder", true);
				exchange.getOut().setHeader("orderUpdateStatus", SIAShippingStatus.DELIVERED.toString());
				processShippingDetails(exchange, order);
				List<Long> deliverOrderItemID = new ArrayList<Long>();
				if (isPartialProcessing) {
					deliverOrderItemID = getPartialOrderDeliverdItemId(order);
				} else {
					deliverOrderItemID = getOrderDeliverdItemId(order);
				}
				exchange.setProperty("deliverOrderItemID", deliverOrderItemID);
			} else if (shippingStatus == SIAShippingStatus.SHIPPED) {
				exchange.getOut().setHeader("orderUpdateStatus", SIAShippingStatus.SHIPPED.toString());
				processShippingDetails(exchange, order);
			}
		}
		boolean isOrderUpdateRequired = updatePaymentStatus || updateFeedback || updateShippingDetails;
		if (order.has("needToUpdateOrder")) {
			if (order.getBoolean("needToUpdateOrder") && orderStatus == SIAOrderStatus.CANCELLED) {
				ArrayList<String> orderItemIDs = getOrderItemId(order, exchange);
				String cancelReason = "";
				if (order.has("cancelReason")) {
					cancelReason = order.getString("cancelReason");
				}
				if (order.has("cancelDetails")) {
					cancelReason = order.getJSONObject("cancelDetails").getString("cancelReason");
				}
				exchange.setProperty("orderItemIDs", orderItemIDs);
				if (isPartialProcessing) {
					cancelReason = getPartialCancelReason(orderItems, orderItemIDs);
				}
				exchange.setProperty("orderItemID", getSingleOrderItemId(order));
				JSONObject cancelDetails = LazadaUtil.getCancelDetails(cancelReason);
				exchange.setProperty("cancelReason", cancelDetails.getString("cancelReason"));
				exchange.setProperty("cancelReasonID", cancelDetails.getString("reasonID"));
				exchange.getOut().setHeader("orderUpdateStatus", SIAOrderStatus.CANCELLED.toString());
			}
			isOrderUpdateRequired = order.getBoolean("needToUpdateOrder");
		}
		boolean isAutoPackOrder = exchange.getProperty("isAutoPackOrder", Boolean.class);
		boolean isAutoAcceptOrder = exchange.getProperty("isAutoAcceptOrder", Boolean.class);
		if (isOrderUpdateRequired && (isAutoPackOrder || isAutoAcceptOrder)) {
			// Note: checking whether duplicate request coming or not
			isOrderUpdateRequired = LazadaUtil.checkIsEligibleToProcessOrder(exchange, order);
			if (!isOrderUpdateRequired) {
				log.info("orderID : " + order.getString("orderID") + " - skipped, due to its already called pack api");
				exchange.setProperty("stopProcess", true);
			}
		}
		exchange.getOut().setHeader("isOrderUpdateRequired", isOrderUpdateRequired);

		String countryCode = exchange.getProperty("countryCode", String.class);
		if (isOrderUpdateRequired && countryCode.equalsIgnoreCase("GLOBAL")) {
			String currencyCode = "";
			if (order.has("orderAmount")) {
				currencyCode = order.getJSONObject("orderAmount").getString("currencyCode");
			} else if (order.has("orderSoldAmount")) {
				currencyCode = order.getJSONObject("orderSoldAmount").getString("currencyCode");
			}
			/* Note: changing specific country code & its host url for global account */
			countryCode = LazadaUtil.currencyToCountryCodeMap.get(currencyCode);
			String hostURL = APIUrlConfig.getConfig().getNewAPIUrl(countryCode);

			exchange.setProperty("countryCode", countryCode);
			exchange.setProperty("hostURL", hostURL);
		}

		// Below code will not use now 10/03/16
		if (order.has("sendInvoice")) {
			exchange.setProperty("sendInvoice", order.getBoolean("sendInvoice"));
		}

		exchange.getOut().setBody(order);
	}

	private ArrayList<Long> getPartialOrderDeliverdItemId(JSONObject order) throws JSONException {
		ArrayList<Long> partialOrderItemIDs = new ArrayList<Long>();
		JSONArray orderItemIDs = order.getJSONArray("orderItemIDs");
		for (int i = 0; i < orderItemIDs.length(); i++) {
			Long orderItemID = Long.parseLong(orderItemIDs.getString(i));
			partialOrderItemIDs.add(orderItemID);
		}
		return partialOrderItemIDs;
	}

	private boolean isRepackOrder(JSONObject order, Exchange exchange) throws JSONException {
		if (order.has("orderItemIDs")) {
			List<String> orderItemIDList = new ArrayList<String>();
			JSONArray orderItemIDs = order.getJSONArray("orderItemIDs");
			for (int i = 0; i < orderItemIDs.length(); i++) {
				orderItemIDList.add(orderItemIDs.getString(i));
			}
			JSONArray orderItems = order.getJSONArray("orderItems");
			for (int i = 0; i < orderItems.length(); i++) {
				JSONObject orderItem = orderItems.getJSONObject(i);
				if (orderItemIDList.contains(orderItem.getString("orderItemID"))) {
					if (orderItem.getString("orderStatus").equalsIgnoreCase(SIAOrderStatus.ACCEPTED.toString())
							&& orderItem.has("shippingTrackingDetails")) {
						JSONObject shippingTrackingDetails = orderItem.getJSONObject("shippingTrackingDetails");
						if (shippingTrackingDetails.has("packageID")) {
							exchange.setProperty("packageID", shippingTrackingDetails.getString("packageID"));
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private String getPartialShippingStatus(JSONArray orderItems, ArrayList<String> orderItemIDs, String key)
			throws JSONException {
		for (int i = 0; i < orderItems.length(); i++) {
			JSONObject orderItem = orderItems.getJSONObject(i);
			if (orderItemIDs.contains(orderItem.getString("orderItemID"))) {
				return orderItem.getString(key);
			}
		}
		return null;
	}

	private String getPartialCancelReason(JSONArray orderItems, ArrayList<String> orderItemIDs) throws JSONException {
		String cancelReason = "";
		for (int i = 0; i < orderItems.length(); i++) {
			JSONObject orderItem = orderItems.getJSONObject(i);
			if (orderItemIDs.contains(orderItem.getString("orderItemID"))) {
				if (orderItem.has("cancelReason")) {
					cancelReason = orderItem.getString("cancelReason");
				}
				if (orderItem.has("cancelDetails")) {
					cancelReason = orderItem.getJSONObject("cancelDetails").getString("cancelReason");
				}
				break;
			}
		}
		return cancelReason;
	}

	private void updateStatusToProcessing(JSONObject order) throws JSONException {
		JSONArray orderItems = order.getJSONArray("orderItems");
		Set<String> orderStatuses = new HashSet<String>();
		Set<String> shippingStatuses = new HashSet<String>();
		for (int i = 0; i < orderItems.length(); i++) {
			JSONObject orderItem = orderItems.getJSONObject(i);
			orderItem.put("orderStatus", SIAOrderStatus.PROCESSING.toString());
			orderItem.put("shippingStatus", SIAShippingStatus.READY_TO_SHIP.toString());
			orderStatuses.add(SIAOrderStatus.PROCESSING.toString());
			shippingStatuses.add(SIAShippingStatus.READY_TO_SHIP.toString());
		}
		order.put("orderStatuses", orderStatuses);
		order.put("shippingStatuses", shippingStatuses);
		order.put("orderStatus", SIAOrderStatus.PROCESSING.toString());
		order.put("shippingStatus", SIAShippingStatus.READY_TO_SHIP.toString());
	}

	private void processShippingDetailsForPartialProcessing(Exchange exchange, JSONObject order)
			throws JSONException {
		JSONArray orderItems = order.getJSONArray("orderItems");
		ArrayList<String> orderItemIDs = (ArrayList<String>) exchange.getProperty("orderItemIDs");
		Set<String> orderStatuses = new HashSet<String>();
		Set<String> shippingStatuses = new HashSet<String>();
		for (int i = 0; i < orderItems.length(); i++) {
			JSONObject orderItem = orderItems.getJSONObject(i);
			if (orderItemIDs.contains(orderItem.getString("orderItemID")) && orderItem.has("shippingTrackingDetails")) {
				orderItem.put("orderStatus", SIAOrderStatus.PROCESSING.toString());
				orderItem.put("shippingStatus", SIAShippingStatus.READY_TO_SHIP.toString());
				JSONObject shippingTrackingDetails = orderItem.getJSONObject("shippingTrackingDetails");
				exchange.setProperty("deliveryType", orderItem.getString("deliveryType"));
				if (shippingTrackingDetails.has("courierName")) {
					String courierName = shippingTrackingDetails.getString("courierName");
					exchange.setProperty("courierName", courierName);
				}
				if (shippingTrackingDetails.has("airwayBill")) {
					exchange.setProperty("airwayBill", shippingTrackingDetails.getString("airwayBill"));
				}
			}
			orderStatuses.add(orderItem.getString("orderStatus"));
			shippingStatuses.add(orderItem.getString("shippingStatus"));
		}
		order.put("orderStatuses", orderStatuses);
		order.put("shippingStatuses", shippingStatuses);
		if (orderStatuses.size() == 1) {
			order.put("orderStatus", orderStatuses.iterator().next());
		}
		if (shippingStatuses.size() == 1) {
			order.put("shippingStatus", shippingStatuses.iterator().next());
		}
	}

	private boolean isOrderPartiallyProcessing(JSONObject order) throws JSONException {
		if(order.has("orderItemIDs")) {
			JSONArray orderItemIDs = order.getJSONArray("orderItemIDs");
			JSONArray orderItems = order.getJSONArray("orderItems");
			if (orderItemIDs.length() != orderItems.length()) {
				return true;
			}
		}
		return false;
	}

	private static void processShippingDetails(Exchange exchange, JSONObject order) throws JSONException {
		String shippingProviderList = Config.getConfig().getSiaIntegratedShippingProviderList();
		String lazadaIntegratedShippingCarriers = Config.getConfig().getLazadaIntegratedShippingCarriers();
		boolean siaIntegratedShippingProvider = false, isLazadaIntegratedShippingCarrier = false;
		if (order.has("shippingDetails") && order.getJSONObject("shippingDetails").has("shippingTrackingDetails")) {
			JSONObject shippingTrackingDeatils = order.getJSONObject("shippingDetails").getJSONObject(
					"shippingTrackingDetails");
			exchange.setProperty("deliveryType", getDeliveryTypeFromOrderItems(order.getJSONArray("orderItems")));
			if (shippingTrackingDeatils.has("courierName")) {
				String courierName = shippingTrackingDeatils.getString("courierName");
				exchange.setProperty("courierName", courierName);
				if (shippingProviderList.contains(courierName)) {
					exchange.setProperty("logisticProvider", courierName);
					siaIntegratedShippingProvider = true;
				}
				List<String> shippingLabelOrderItemIds = getShippingLableOrderItemIds(order);
				if (shippingLabelOrderItemIds.size() > 0 || lazadaIntegratedShippingCarriers.contains(courierName)) {
					isLazadaIntegratedShippingCarrier = true;
					exchange.setProperty("shippingLabelOrderItemIds", shippingLabelOrderItemIds);
				}
			}
			if (shippingTrackingDeatils.has("airwayBill")) {
				exchange.setProperty("airwayBill", shippingTrackingDeatils.getString("airwayBill"));
			}
			if (exchange.getProperty("isProcessingDelivered", Boolean.class)
					&& !shippingTrackingDeatils.has("sofReferenceNumber")) {
				log.error("sofReferenceNumber is not present for orderID: " + order.getString("orderID")
						+ " accountNumber: " + exchange.getProperty("accountNumber", String.class) + ", nickNameID: "
						+ exchange.getProperty("nickNameID", String.class));
			}
			if (shippingTrackingDeatils.has("sofReferenceNumber")) {
				exchange.setProperty("sofReferenceNumber", shippingTrackingDeatils.getString("sofReferenceNumber"));
			}
		}
		exchange.setProperty("siaIntegratedShippingProvider", siaIntegratedShippingProvider);
		exchange.setProperty("isLazadaIntegratedShippingCarrier", isLazadaIntegratedShippingCarrier);
	}

	private static List<String> getShippingLableOrderItemIds(JSONObject order) throws JSONException {
		List<String> orderItemIds = new ArrayList<String>();
		if (order.has("orderItems")) {
			JSONArray orderItems = order.getJSONArray("orderItems");
			for (int i = 0; i < orderItems.length(); i++) {
				JSONObject orderItem = (JSONObject) orderItems.get(i);
				if (orderItem.has("isSellerOwnFleet") && !orderItem.getBoolean("isSellerOwnFleet")) {
					orderItemIds.add(orderItem.getString("orderItemID"));
				}
			}
		}
		return orderItemIds;
	}

	private static ArrayList<String> getOrderItemId(JSONObject order, Exchange exchange) throws JSONException {
		ArrayList<String> orderItemIds = new ArrayList<String>();
		ArrayList<String> freeGiftIds = new ArrayList<String>();
		if (order.has("orderItemIDs")) {
			JSONArray orderItems = order.getJSONArray("orderItemIDs");
			for (int i = 0; i < orderItems.length(); i++) {
				JSONArray orderItemDetails = order.getJSONArray("orderItems");
				boolean isFreeGift = false;
				for (int j = 0; j < orderItemDetails.length(); j++) {
					if (orderItemDetails.getJSONObject(j).has("orderItemID")
							&& orderItemDetails.getJSONObject(j).getString("orderItemID").equals(
									orderItems.getString(i))
							&& orderItemDetails.getJSONObject(j).has("isFreeGift")
							&& orderItemDetails.getJSONObject(j).getBoolean("isFreeGift")) {
						isFreeGift = true;
						break;
					}
				}
				if (isFreeGift) {
					freeGiftIds.add(orderItems.getString(i));
					continue;
				}
				orderItemIds.add(orderItems.getString(i));
			}
			exchange.setProperty("freeGiftIDs", freeGiftIds);
			return orderItemIds;
		}
		JSONArray orderItems = order.getJSONArray("orderItems");
		for (int i = 0; i < orderItems.length(); i++) {
			if (orderItems.getJSONObject(i).has("isFreeGift") && orderItems.getJSONObject(i).getBoolean("isFreeGift")) {
				freeGiftIds.add(orderItems.getJSONObject(i).getString("orderItemID"));
				continue;
			}
			orderItemIds.add(orderItems.getJSONObject(i).getString("orderItemID"));
		}
		exchange.setProperty("freeGiftIDs", freeGiftIds);
		return orderItemIds;
	}

	private static ArrayList<Long> getOrderDeliverdItemId(JSONObject order) throws JSONException {
		ArrayList<Long> orderItemIds = new ArrayList<Long>();
		JSONArray orderItems = order.getJSONArray("orderItems");
		for (int i = 0; i < orderItems.length(); i++) {
			JSONObject orderItem = orderItems.getJSONObject(i);
			if(orderItem.has("orderStatus")){
				String orderStatus = orderItem.getString("orderStatus");
				if (orderStatus.equals(SIAOrderStatus.PROCESSING.toString())
						|| orderStatus.equals(SIAOrderStatus.DELIVERY_FAILED.toString())) {
					orderItemIds.add(Long.parseLong(orderItems.getJSONObject(i).getString("orderItemID")));
				}
			}
		}
		return orderItemIds;
	}

	private static String getSingleOrderItemId(JSONObject order) throws JSONException {
		JSONArray orderItems = order.getJSONArray("orderItems");
		return orderItems.getJSONObject(0).getString("orderItemID");
	}

	private static String getDeliveryTypeFromOrderItems(JSONArray orderItems) throws JSONException {
		// Multiple items in single order have single delivery type
		// So that get (0)
		return orderItems.getJSONObject(0).getString("deliveryType");
	}
}