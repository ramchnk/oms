package com.sellinall.lazada.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.CurrencyUtil;

public class ConstructMessageForFinance implements Processor {
	static Logger log = Logger.getLogger(ConstructMessageForFinance.class.getName());

	public void process(Exchange exchange) throws Exception {
		if (exchange.getProperty("requestType", String.class).equals("getSettlementSummary")) {
			/* Here constructing settlement summary message */
			JSONObject message = new JSONObject();
			if (exchange.getProperties().containsKey("merchantID")) {
				message.put("merchantID", exchange.getProperty("merchantID", String.class));
			}
			if (exchange.getProperties().containsKey("isSellerInitiated")) {
				message.put("isSellerInitiated", exchange.getProperty("isSellerInitiated", Boolean.class));
			}
			message.put("accountNumber", exchange.getProperty("accountNumber", String.class));
			message.put("nickNameID", exchange.getProperty("nickNameID", String.class));
			message.put("nickName", exchange.getProperty("nickName", String.class));
			message.put("documentID", exchange.getProperty("documentID", String.class));
			message.put("countryCode", exchange.getProperty("countryCode", String.class));
			message.put("requestType", "getSettlementSummary");
			if (exchange.getProperties().containsKey("processOrdersWithSKUOnly")
					&& exchange.getProperty("processOrdersWithSKUOnly", Boolean.class)) {
				message.put("commonStorePresent", true);
			}
			if (exchange.getProperty("isSuccessResponse", Boolean.class)) {
				message.put("status", "success");
				message.put("settlementSummary", exchange.getProperty("settlementSummary", JSONArray.class));
			} else {
				message.put("status", "failed");
			}
			exchange.getOut().setBody(message);
		} else if (exchange.getProperty("requestType", String.class).equals("getSettlementDetails")) {
			if (exchange.getProperties().containsKey("updateSettlementStatus")
					&& exchange.getProperty("updateSettlementStatus", Boolean.class)) {
				/*
				 * Here constructing paging message to update settlement summary
				 * record
				 */
				JSONObject message = new JSONObject();
				message.put("accountNumber", exchange.getProperty("accountNumber", String.class));
				message.put("nickNameID", exchange.getProperty("nickNameID", String.class));
				message.put("documentID", exchange.getProperty("documentID", String.class));
				message.put("countryCode", exchange.getProperty("countryCode", String.class));
				message.put("requestType", "updateSettlementSummary");

				message.put("noOfRows", exchange.getProperty("noOfRows", Integer.class));
				message.put("noOfRowsProcessed", exchange.getProperty("noOfRowsProcessed", Integer.class));
				message.put("noOfOrders", exchange.getProperty("noOfOrders", Integer.class));
				message.put("noOfOrdersProcessed", exchange.getProperty("noOfOrdersProcessed", Integer.class));
				message.put("noOfOrdersSkipped", exchange.getProperty("noOfOrdersSkipped", Integer.class));
				message.put("processedOrders", new JSONArray(exchange.getProperty("processedOrders", Set.class)));
				message.put("skippedOrders", new JSONArray(exchange.getProperty("skippedOrders", Set.class)));
				message.put("noOfOrderItems", exchange.getProperty("noOfOrderItems", Integer.class));
				message.put("noOfOrderItemsProcessed", exchange.getProperty("noOfOrderItemsProcessed", Integer.class));
				message.put("noOfOrderItemsSkipped", exchange.getProperty("noOfOrderItemsSkipped", Integer.class));
				message.put("responseSettlementAmount",
						CurrencyUtil.getJSONAmountObject(exchange.getProperty("totalProcessedAmount", Long.class),
								exchange.getProperty("currencyCode", String.class)));
				if (exchange.getProperty("isSuccessResponse", Boolean.class)) {
					if (exchange.getProperty("isLastPage", Boolean.class)) {
						message.put("status", "completed");
					} else {
						message.put("status", "processing");
					}
				} else {
					message.put("status", "failed");
				}
				exchange.getOut().setBody(message);
			} else {
				/* Here constructing settlement details message */
				JSONArray settlementDetails = exchange.getProperty("settlementDetails", JSONArray.class);
				Map<String, Set<String>> orderMap = exchange.getProperty("orderMap", Map.class);
				BasicDBObject channelObj = exchange.getProperty("userChannel", BasicDBObject.class);
				String userID = channelObj.getString("userID");

				int noOfRows = exchange.getProperty("noOfRows", Integer.class);
				int noOfRowsProcessed = exchange.getProperty("noOfRowsProcessed", Integer.class);
				long totalProcessedAmount = exchange.getProperty("totalProcessedAmount", Long.class);
				Set<String> processedOrders = exchange.getProperty("processedOrders", Set.class);
				Set<String> processedOrderItems = exchange.getProperty("processedOrderItems", Set.class);
				Set<String> skippedOrders = exchange.getProperty("skippedOrders", Set.class);
				Set<String> skippedOrderItems = exchange.getProperty("skippedOrderItems", Set.class);

				List<JSONObject> messages = new ArrayList<JSONObject>();
				for (int i = 0; i < settlementDetails.length(); i++) {
					JSONObject settlementDetailObj = settlementDetails.getJSONObject(i);
					String orderID = "", orderItemID = "", feeName = "", transactionType = "";

					JSONObject message = new JSONObject();
					message.put("site", "lazada");
					message.put("documentID", exchange.getProperty("documentID", String.class));
					message.put("countryCode", exchange.getProperty("countryCode", String.class));
					message.put("requestType", "getSettlementDetails");
					message.put("storeUniqueIdentifier", userID);

					Iterator keys = settlementDetailObj.keys();
					while (keys.hasNext()) {
						String key = (String) keys.next();
						if (key.equals("order_no")) {
							orderID = settlementDetailObj.getString(key);
							message.put("orderID", orderID);
						} else if (key.equals("orderItem_no")) {
							orderItemID = settlementDetailObj.getString(key);
							message.put("orderItemID", orderItemID);
						} else if (key.equals("transaction_type")) {
							transactionType = settlementDetailObj.getString(key);
							message.put("transactionType", transactionType);
						} else if (key.equals("fee_name")) {
							continue;
						} else if (key.equals("statement")) {
							message.put("transactionPeriod", settlementDetailObj.getString(key));
						} else if (key.equals("fee_type")) {
							message.put("lazadaFeeType", settlementDetailObj.getString(key));
						} else {
							message.put(key, settlementDetailObj.getString(key));
						}
					}
					
					if (settlementDetailObj.has("fee_name")) {
						feeName = settlementDetailObj.getString("fee_name");
						message.put("feeName", feeName);
						message.put("feeType", getFeeType(orderID, orderItemID, transactionType, feeName));
					}
					
					/*
					 * for some feeName orderID is present but orderItemID not present, checked &
					 * confirmed that reference value matched with orderItemID, so updating
					 * reference value as orderItemID. To save time in downstream changes, we did
					 * here.
					 */
					if (!orderID.isEmpty() && orderItemID.isEmpty() && settlementDetailObj.has("reference")) {
						message.put("orderItemID", settlementDetailObj.getString("reference"));
					}
					if (exchange.getProperties().containsKey("processOrdersWithSKUOnly")
							&& exchange.getProperty("processOrdersWithSKUOnly", Boolean.class)) {
						message.put("commonStorePresent", true);
					}
					if (!orderID.isEmpty()) {
						if (orderMap.containsKey(orderID)) {
							Set<String> orderItemIDs = orderMap.get(orderID);
							if (orderItemIDs.contains(orderItemID) || orderItemID.isEmpty()) {
								message.put("accountNumber", exchange.getProperty("accountNumber", String.class));
								message.put("nickNameID", exchange.getProperty("nickNameID", String.class));
								processedOrders.add(orderID);
								if (!orderItemID.isEmpty()) {
									processedOrderItems.add(orderItemID);
								}
								totalProcessedAmount += CurrencyUtil.convertAmountToSIAFormat(
										Double.parseDouble(settlementDetailObj.getString("amount")));
								noOfRowsProcessed++;
							} else {
								skippedOrderItems.add(orderItemID);
							}
						} else {
							/*
							 * if orderID not present in orderMap, then this
							 * order is not available in our side for this
							 * seller, so skipping this row item.
							 */
							skippedOrders.add(orderID);
						}
					} else {
						/*
						 * If Fee name is sponsored fee, then this row item
						 * doesn't have orderID. so we just processing this row
						 * item without order check.
						 */
						message.put("accountNumber", exchange.getProperty("accountNumber", String.class));
						message.put("nickNameID", exchange.getProperty("nickNameID", String.class));
						totalProcessedAmount += CurrencyUtil
								.convertAmountToSIAFormat(Double.parseDouble(settlementDetailObj.getString("amount")));
						noOfRowsProcessed++;
					}
					messages.add(message);
					noOfRows++;
				}
				exchange.getOut().setBody(messages);
				exchange.setProperty("noOfRows", noOfRows);
				exchange.setProperty("noOfRowsProcessed", noOfRowsProcessed);
				exchange.setProperty("processedOrders", processedOrders);
				exchange.setProperty("skippedOrders", skippedOrders);
				exchange.setProperty("processedOrderItems", processedOrderItems);
				exchange.setProperty("skippedOrderItems", skippedOrderItems);
				exchange.setProperty("noOfOrdersProcessed", processedOrders.size());
				exchange.setProperty("noOfOrdersSkipped", skippedOrders.size());
				exchange.setProperty("noOfOrders", processedOrders.size() + skippedOrders.size());
				exchange.setProperty("noOfOrderItemsProcessed", processedOrderItems.size());
				exchange.setProperty("noOfOrderItemsSkipped", skippedOrderItems.size());
				exchange.setProperty("noOfOrderItems", processedOrderItems.size() + skippedOrderItems.size());
				exchange.setProperty("totalProcessedAmount", totalProcessedAmount);
			}
		}
	}

	private String getFeeType(String orderID, String orderItemID, String transactionType, String feeName) {
		feeName = LazadaUtil.removeSpecialCharacters(feeName);
		if (orderID.isEmpty() && (orderItemID.isEmpty() || orderItemID.equals("null"))) {
			return "others";
		} else if (transactionType.toLowerCase().contains("claim")
				|| Arrays.asList(Config.getConfig().getClaimFeeNames().split(",")).contains(feeName)) {
			return "claim";
		} else if (transactionType.toLowerCase().contains("orders")
				|| Arrays.asList(Config.getConfig().getOrderFeeNames().split(",")).contains(feeName)) {
			return "order";
		} else if (transactionType.toLowerCase().contains("refunds")
				|| Arrays.asList(Config.getConfig().getRefundFeeNames().split(",")).contains(feeName)) {
			return "refund";
		} else if (orderID.isEmpty()) {
			return "others";
		}
		log.error("unknown feeType found for orderID : " + orderID + ", transactionType : " + transactionType
				+ ", feeName : " + feeName);
		return "";
	}

}
