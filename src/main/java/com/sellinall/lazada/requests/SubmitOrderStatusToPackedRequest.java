package com.sellinall.lazada.requests;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.enums.OrderUpdateStatus;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.SIAShippingStatus;

public class SubmitOrderStatusToPackedRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitOrderStatusToPackedRequest.class.getName());
	static int maxRetryCount = 3;
	public static final List<String> VALID_FAILURE_REASONS = Arrays
			.asList("[TFS seller can't change shipment provider.]", "E082: All order items must have status Pending.");

	public void process(Exchange exchange) throws Exception {
		int retryCount = 1;
		callAPIToReadyToShip(exchange, retryCount);
	}

	private void callAPIToReadyToShip(Exchange exchange , int retryCount) throws IOException, JSONException {
		String response = null;
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		ArrayList<String> orderItemIDs = new ArrayList<String>();
		boolean isUpdateOrder = true;
		if (exchange.getProperties().containsKey("requestType")
				&& exchange.getProperty("requestType", String.class).equals("getDocuments")) {
			isUpdateOrder = false;
			orderItemIDs = (ArrayList<String>) exchange.getProperty("orderItemIDsForGeneratingAWB");
		} else {
			orderItemIDs = (ArrayList<String>) exchange.getProperty("orderItemIDs");
		}
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("order_item_ids", orderItemIDs.toString());
		map.put("access_token", accessToken);
		map.put("delivery_type", "dropship");
		String queryParams = "&delivery_type=dropship";
		JSONObject order = new JSONObject();
		if (exchange.getProperties().containsKey("order")) {
			order = exchange.getProperty("order", JSONObject.class);
		}
		if (exchange.getProperties().containsKey("courierName")) {
			String courierName = exchange.getProperty("courierName", String.class);
			exchange.setProperty("shippingProvider", courierName);
			String countryCode = exchange.getProperty("countryCode", String.class);
			// if isSellerOwnFleet not required /order/pack api directly call
			// ready to ship api
			if (exchange.getProperties().containsKey("countryCode")
					&& ((order.has("isSellerOwnFleet") && order.getBoolean("isSellerOwnFleet"))
							|| (exchange.getProperties().containsKey("isSellerOwnFleet")
									&& exchange.getProperty("isSellerOwnFleet", Boolean.class)))) {
				if (countryCode.equals("MY")) {
					courierName = "Seller Own";
				} else if (countryCode.equals("ID")) {
					//get shipping provider type from oder mostly come seller_own_fleet
					String shippingProviderType = null ;
					if(orderItemIDs.size() > 0 ) {
						shippingProviderType = LazadaUtil.getShippingProviderType(order, orderItemIDs.get(0));
					}
					courierName = shippingProviderType != null ? shippingProviderType : "Seller Own";
				} else if (countryCode.equals("SG")) {
					if (exchange.getProperties().containsKey("isDeliveredBySellerEnabled")
							&& exchange.getProperty("isDeliveredBySellerEnabled", Boolean.class)) {
						courierName = "Delivered by Seller";
					} else {
						courierName = "Seller Own Fleet";
					}
				}
				exchange.setProperty("courierName", courierName);
				exchange.setProperty("updateStatus", OrderUpdateStatus.COMPLETE.toString());
				List<String> airwayBillNumbers = new ArrayList<String>();
				if (exchange.getProperties().containsKey("airwayBill")) {
					airwayBillNumbers.add(exchange.getProperty("airwayBill", String.class));
				}
				exchange.setProperty("airwayBillNumbers", airwayBillNumbers);
				return;
			}
			map.put("shipping_provider", courierName);
			queryParams += "&shipping_provider=" + URLEncoder.encode(courierName, "UTF-8");
		} else if (exchange.getProperties().containsKey("preferredLogistic")) {
			String courierName = exchange.getProperty("preferredLogistic", String.class);
			map.put("shipping_provider", courierName);
			queryParams += "&shipping_provider=" + URLEncoder.encode(courierName, "UTF-8");
		}
		queryParams += "&order_item_ids=" + URLEncoder.encode(orderItemIDs.toString(), "UTF-8");
		String logMessage = "";
		if(order.has("orderID")) {
			logMessage = "orderId : " + order.getString("orderID");
		} else {
			logMessage = "orderItemId : " + orderItemIDs;
		}
		JSONObject serviceResponse = new JSONObject();
		try {
			String clientID = Config.getConfig().getLazadaClientID();
			String clientSecret = Config.getConfig().getLazadaClientSecret();
			response = NewLazadaConnectionUtil.callAPI(hostURL, "/order/pack", accessToken, map, "", queryParams,
					"POST", clientID, clientSecret);
			serviceResponse = new JSONObject(response);
			if (serviceResponse.has("code") && ! serviceResponse.getString("code").equals("0")
					&& retryCount <= maxRetryCount) {
				if (!serviceResponse.has("message")
						|| !VALID_FAILURE_REASONS.contains(serviceResponse.getString("message"))) {
					log.error("Getting invalid response from order details api for " + logMessage + ", accountNumber : "
							+ exchange.getProperty("accountNumber") + ", nickNameID : "
							+ exchange.getProperty("nickNameID") + ", retryCount : " + retryCount + " & response : "
							+ response);
					retryCount++;
					Thread.sleep(1000);
					callAPIToReadyToShip(exchange, retryCount);
					return;
				}
			}
			if (serviceResponse.has("code") && serviceResponse.getString("code").equals("0")) {
				log.info("pack API response for " + logMessage + ", accountNumber: "
						+ exchange.getProperty("accountNumber", String.class) + ", nickNameID: "
						+ exchange.getProperty("nickNameID", String.class) + " from SubmitOrderStatusToPackedRequest: "
						+ serviceResponse);
				JSONObject data = serviceResponse.getJSONObject("data");
				JSONArray orderItemsFromResponse = new JSONArray();
				if (data.has("order_items")) {
					orderItemsFromResponse = data.getJSONArray("order_items");
				}
				boolean orderHasMultiplePackages = false;
				boolean isPartialProcessing = false;
				if (exchange.getProperties().containsKey("isPartialProcessing")) {
					isPartialProcessing = exchange.getProperty("isPartialProcessing", Boolean.class);
				}
				Map<String, BasicDBObject> orderItemsMap = new HashMap<String, BasicDBObject>();
				Map<String, BasicDBObject> trackingNumberMap = getTrackingNumberMap(orderItemsFromResponse, orderItemsMap);
				exchange.setProperty("airwayBillNumbers", new ArrayList<String>(orderItemsMap.keySet()));
				if (orderItemsMap.size() > 1) {
					orderHasMultiplePackages = true;
					exchange.setProperty("orderItemsMap", orderItemsMap);
				}
				exchange.setProperty("orderHasMultiplePackages", orderHasMultiplePackages);
				if(isUpdateOrder) {
					if (!exchange.getProperties().containsKey("airwayBill") || orderHasMultiplePackages || isPartialProcessing) {
						JSONArray orderItems = new JSONArray();
						if(order.has("orderItems")) {
							orderItems = order.getJSONArray("orderItems");
						}else {
							orderItems = exchange.getProperty("orderItems", JSONArray.class);
						}
						updateTrackingDetailsInOrderItem(serviceResponse, orderItems, exchange,
								trackingNumberMap);
					}
					updateTrackingDetailsinOrder(exchange, order, serviceResponse, trackingNumberMap);
					exchange.setProperty("updateStatus", OrderUpdateStatus.COMPLETE.toString());
				} else if (exchange.getProperties().containsKey("isAutoPackOrders")
						&& exchange.getProperty("isAutoPackOrders", Boolean.class)
						&& exchange.getProperties().containsKey("isNewOrder")
						&& exchange.getProperty("isNewOrder", Boolean.class)) {
					JSONArray orderItems = exchange.getProperty("orderItems", JSONArray.class);
					updateTrackingDetailsInOrderItem(serviceResponse, orderItems, exchange, trackingNumberMap);
					exchange.setProperty("orderItems", orderItems);
				} else {
					Map<String, JSONObject> orderMsgNeedToPublish = new HashMap<String, JSONObject>();
					if (exchange.getProperties().containsKey("orderMsgNeedToPublish")) {
						orderMsgNeedToPublish = exchange.getProperty("orderMsgNeedToPublish", Map.class);
					}
					List<JSONObject> orderList = new ArrayList<JSONObject>(orderMsgNeedToPublish.values());
					for (int i = 0; i < orderList.size(); i++) {
						JSONObject orderDetail = orderList.get(i);
						updateTrackingDetailsInOrderItem(serviceResponse, orderDetail.getJSONArray("orderItems"),
								exchange, trackingNumberMap);
						updateTrackingDetailsinOrder(exchange, orderDetail, serviceResponse, trackingNumberMap);
						orderDetail.put("updateStatus", OrderUpdateStatus.COMPLETE.toString());
					}
					exchange.setProperty("orderList", orderList);
					exchange.setProperty("updateStatus", OrderUpdateStatus.COMPLETE.toString());
				}
				return;
			}
			log.error("SubmitOrderStatusToPackedRequest failed for " + logMessage + " for nickNameId:"
					+ exchange.getProperty("nickNameID", String.class) + " for accountNumber: "
					+ exchange.getProperty("accountNumber", String.class) + " request" + queryParams.toString()
					+ "  and response: " + response);
			exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
			if(serviceResponse.has("message")) {
				exchange.setProperty("failureReason", serviceResponse.getString("message"));
			}
		} catch (Exception e) {
			log.error("Error occurred during SubmitOrderStatusToPackedRequest for " + logMessage + " for nickNameId:"
					+ exchange.getProperty("nickNameID", String.class) + " for accountNumber: "
					+ exchange.getProperty("accountNumber", String.class) + " request" + queryParams.toString()
					+ " and response: " + response);
			e.printStackTrace();
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
				&& order.getString("orderStatus").equals(SIAOrderStatus.PROCESSING.toString()))
				|| (exchange.getProperties().containsKey("isAutoPackOrder")
						&& exchange.getProperty("isAutoPackOrder", Boolean.class))) {
			LazadaUtil.setCurrentOrderStatus(exchange, order, orderItemIDs, false);
		}
	}

	private Map<String, BasicDBObject> getTrackingNumberMap(JSONArray orderItems,
			Map<String, BasicDBObject> orderItemsMap) throws JSONException {
		Map<String, BasicDBObject> trackingNumberMap = new HashMap<String, BasicDBObject>();
		for (int i = 0; i < orderItems.length(); i++) {
			JSONObject orderItem = orderItems.getJSONObject(i);
			String airwayBill = orderItem.getString("tracking_number");
			String courierName = orderItem.getString("shipment_provider");
			String orderItemID = orderItem.getString("order_item_id");
			String packageID = orderItem.getString("package_id");

			BasicDBObject trackingObj = new BasicDBObject();
			trackingObj.put("airwayBill", airwayBill);
			trackingObj.put("courierName", courierName);
			trackingObj.put("packageID", packageID);
			trackingNumberMap.put(orderItemID, trackingObj);

			if (orderItemsMap.containsKey(airwayBill)) {
				BasicDBObject orderItemDetails = orderItemsMap.get(airwayBill);
				List<String> orderItemIDs = (List<String>) orderItemDetails.get("orderItemIDs");
				orderItemIDs.add(orderItemID);
			} else {
				List<String> orderItemIDs = new ArrayList<String>();
				orderItemIDs.add(orderItemID);

				BasicDBObject orderItemDetails = new BasicDBObject();
				orderItemDetails.put("courierName", courierName);
				orderItemDetails.put("airwayBill", airwayBill);
				orderItemDetails.put("orderItemIDs", orderItemIDs);

				orderItemsMap.put(airwayBill, orderItemDetails);
			}
		}
		return trackingNumberMap;
	}

	private void updateTrackingDetailsinOrder(Exchange exchange, JSONObject order, JSONObject serviceResponse,
			Map<String, BasicDBObject> trackingNumberMap) throws JSONException {
		if (serviceResponse.has("data")) {
			JSONObject data = serviceResponse.getJSONObject("data");
			if (data.has("order_items")) {
				JSONArray orderItemsFromResponse = data.getJSONArray("order_items");
				JSONObject orderItem = orderItemsFromResponse.getJSONObject(0);
				if (orderItem.has("tracking_number") && !orderItem.getString("tracking_number").isEmpty()
						&& (orderItem.has("shipment_provider")
								&& !orderItem.getString("shipment_provider").equals("Seller Own")
								&& !orderItem.getString("shipment_provider").equals("Seller Own Fleet")
								&& !orderItem.getString("shipment_provider").equals("SOFP")
								&& !orderItem.getString("shipment_provider").equals("Delivered by Seller"))) {
					String airwayBill = "";
					String courierName = "";
					String packageID = "";
					if (order.has("shippingDetails")) {
						JSONObject shippingDetails = order.getJSONObject("shippingDetails");
						Set<BasicDBObject> shippingTrackingDetailsListResponse = new HashSet<BasicDBObject>();
						if (shippingDetails.has("shippingTrackingDetailsList")) {
							JSONArray shippingTrackingDetailsListRequest = shippingDetails
									.getJSONArray("shippingTrackingDetailsList");
							for (int j = 0; j < shippingTrackingDetailsListRequest.length(); j++) {
								JSONObject shippingTrackingObj = shippingTrackingDetailsListRequest.getJSONObject(j);
								if (shippingTrackingObj.length() > 0) {
									shippingTrackingDetailsListResponse
											.add((BasicDBObject) JSON.parse(shippingTrackingObj.toString()));
								}
							}
						}
						JSONArray orderItems = order.getJSONArray("orderItems");
						for (int i = 0; i < orderItems.length(); i++) {
							orderItem = orderItems.getJSONObject(i);
							String orderItemID = orderItem.getString("orderItemID");
							if (trackingNumberMap.containsKey(orderItemID)) {
								BasicDBObject trackingObj = trackingNumberMap.get(orderItemID);
								airwayBill = trackingObj.getString("airwayBill");
								courierName = trackingObj.getString("courierName");
								packageID = trackingObj.getString("packageID");
								shippingTrackingDetailsListResponse.add(trackingObj);
							}
						}
						JSONArray shippingTrackingDetailsArray = new JSONArray();
						for (BasicDBObject shippingTrackingDetail : shippingTrackingDetailsListResponse) {
							shippingTrackingDetailsArray.put(LazadaUtil.parseToJsonObject(shippingTrackingDetail));
						}
						shippingDetails.put("shippingTrackingDetailsList", shippingTrackingDetailsArray);
						if (shippingDetails.has("shippingTrackingDetails")) {
							JSONObject shippingTrackingDetails = shippingDetails
									.getJSONObject("shippingTrackingDetails");
							shippingTrackingDetails.put("courierName", courierName);
							shippingTrackingDetails.put("airwayBill", airwayBill);
							shippingTrackingDetails.put("packageID", packageID);
						} else {
							JSONObject shippingTrackingDetails = new JSONObject();
							shippingTrackingDetails.put("courierName", courierName);
							shippingTrackingDetails.put("airwayBill", airwayBill);
							shippingTrackingDetails.put("packageID", packageID);
							shippingDetails.put("shippingTrackingDetails", shippingTrackingDetails);
						}
					}
					exchange.setProperty("airwayBill", airwayBill);
					exchange.setProperty("courierName", courierName);
				}
			}
		}
	}

	private void updateTrackingDetailsInOrderItem(JSONObject serviceResponse, JSONArray orderItems, Exchange exchange,
			Map<String, BasicDBObject> trackingNumberMap) throws JSONException {
		if (serviceResponse.has("data")) {
			JSONObject data = serviceResponse.getJSONObject("data");
			if (data.has("order_items")) {
				JSONArray orderItemsResponse = data.getJSONArray("order_items");
				JSONObject orderItemResponse = orderItemsResponse.getJSONObject(0);
				if (orderItemResponse.has("tracking_number")
						&& !orderItemResponse.getString("tracking_number").isEmpty()
						&& (orderItemResponse.has("shipment_provider")
								&& !orderItemResponse.getString("shipment_provider").equals("Seller Own")
								&& !orderItemResponse.getString("shipment_provider").equals("Seller Own Fleet")
								&& !orderItemResponse.getString("shipment_provider").equals("SOFP")
								&& !orderItemResponse.getString("shipment_provider").equals("Delivered by Seller"))) {
					String airwayBill = "";
					ArrayList<String> orderItemIDs = new ArrayList<String>();
					if (exchange.getProperties().containsKey("requestType")
							&& exchange.getProperty("requestType", String.class).equals("getDocuments")) {
						orderItemIDs = (ArrayList<String>) exchange.getProperty("orderItemIDsForGeneratingAWB");
					} else {
						orderItemIDs = (ArrayList<String>) exchange.getProperty("orderItemIDs");
					}
					Set<BasicDBObject> shippingTrackingDetailsSet = new HashSet<BasicDBObject>();
					for (int i = 0; i < orderItems.length(); i++) {
						JSONObject orderItem = orderItems.getJSONObject(i);
						String orderItemID = orderItem.getString("orderItemID");
						if (orderItemIDs.contains(orderItemID)) {
							JSONObject shippingTrackingDetails = new JSONObject();
							if (orderItem.has("shippingTrackingDetails")) {
								shippingTrackingDetails = orderItem.getJSONObject("shippingTrackingDetails");
							}
							if(trackingNumberMap.containsKey(orderItemID)) {
								BasicDBObject trackingObj = trackingNumberMap.get(orderItemID);
								airwayBill = trackingObj.getString("airwayBill");
								shippingTrackingDetails.put("airwayBill", trackingObj.getString("airwayBill"));
								shippingTrackingDetails.put("courierName", trackingObj.getString("courierName"));
								shippingTrackingDetails.put("packageID", trackingObj.getString("packageID"));
								orderItem.put("shippingTrackingDetails", shippingTrackingDetails);
								if (exchange.getProperties().containsKey("isAutoPackOrders")
										&& exchange.getProperty("isAutoPackOrders", Boolean.class)
										&& exchange.getProperties().containsKey("isNewOrder")
										&& exchange.getProperty("isNewOrder", Boolean.class)) {
									shippingTrackingDetailsSet
											.add((BasicDBObject) JSON.parse(shippingTrackingDetails.toString()));
									exchange.setProperty("trackingCode", airwayBill);
									exchange.setProperty("shippingProviderName", trackingObj.getString("courierName"));
								}
							}
						} else if (exchange.getProperty("isRepackOrder", Boolean.class)) {
							orderItem.remove("shippingTrackingDetails");
						}
					}
					if (shippingTrackingDetailsSet.size() > 0) {
						JSONArray shippingTrackingDetailsList = new JSONArray();
						for (BasicDBObject shippingTrackingDetail : shippingTrackingDetailsSet) {
							shippingTrackingDetailsList.put(LazadaUtil.parseToJsonObject(shippingTrackingDetail));
						}
						exchange.setProperty("shippingTrackingDetailsList", shippingTrackingDetailsList);
					}
					exchange.setProperty("airwayBill", airwayBill);
				}
			}
		}
	}
}