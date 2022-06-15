package com.sellinall.lazada.response;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.mudra.sellinall.config.APIUrlConfig;
import com.mudra.sellinall.config.Config;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.enums.SIAOrderStatus;

public class UpdateReservedStockInListings implements Processor {
	static Logger log = Logger.getLogger(UpdateReservedStockInListings.class.getName());
	static final int MAX_RETRY_COUNT = 3;

	public void process(Exchange exchange) throws Exception {
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		JSONArray orderItems = exchange.getProperty("orderItems", JSONArray.class);
		String nickNameID = getNicknameId((DBObject) exchange.getProperty("UserDetails"));
		List<String> nickNameIDs = new ArrayList<String>();
		nickNameIDs.add(nickNameID);

		DBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		searchQuery.put("lazada.nickNameID", new BasicDBObject("$in", nickNameIDs));

		for (int i = 0; orderItems.length() > i; i++) {
			JSONObject orderItem = (JSONObject) orderItems.get(i);
			String orderItemID = orderItem.getString("orderItemID");
			if (!orderItem.has("SKU")) {
				continue;
			}
			searchQuery.put("customSKU", orderItem.getString("customSKU"));
			searchQuery.put("SKU", new BasicDBObject("$ne", orderItem.get("SKU")));

			BasicDBObject updateObject = new BasicDBObject();
			BasicDBObject update = new BasicDBObject();
			BasicDBObject reservedQuantities = new BasicDBObject();

			List<DBObject> inventoryDB = table.find(searchQuery).toArray();

			Boolean isEligibleToUpdatReservedQuantities = eligibleToUpdatReservedQuantities(inventoryDB,
					exchange.getProperty("orderID", String.class), orderItem.getString("orderItemID"));
			if (isEligibleToUpdatReservedQuantities && orderItem.has("orderStatus")
					&& orderItem.get("orderStatus").equals(SIAOrderStatus.INITIATED)) {
				update.put("reservedQuantity", orderItem.getInt("quantity"));
				update.put("promotionID", exchange.getProperty("orderID"));
				update.put("orderItemID", orderItem.get("orderItemID"));
				update.put("createdTime", System.currentTimeMillis() / 1000L);

				BasicDBList list = new BasicDBList();
				list.add(update);

				BasicDBObject reservedQuantitiesList = new BasicDBObject("$each", list);
				reservedQuantities.put("reservedQuantities", reservedQuantitiesList);
				updateObject.put("$push", reservedQuantities);
				table.updateMulti(searchQuery, updateObject);
				log.info("reserved stock added searchQuery:" + searchQuery + " updateObject:" + updateObject);

			} else if (orderItem.has("orderStatus") && (orderItem.get("orderStatus").equals(SIAOrderStatus.CANCELLED)
					|| orderItem.get("orderStatus").equals(SIAOrderStatus.PROCESSING))) {
				update.put("promotionID", exchange.getProperty("orderID"));
				update.put("orderItemID", orderItem.get("orderItemID"));
				reservedQuantities.put("reservedQuantities", update);
				updateObject.put("$pull", reservedQuantities);
				WriteResult updateResult = table.updateMulti(searchQuery, updateObject);
				log.info("reserved stock removed searchQuery:" + searchQuery + " updateObject:" + updateObject
						+ " updateResult:" + updateResult.getN());

				// update quantity in marketplace
				if (updateResult.getN() != 0 && orderItem.get("orderStatus").equals(SIAOrderStatus.PROCESSING)) {
					updateQuantityInMarketPlace(exchange, inventoryDB, orderItemID, nickNameID,
							orderItem.getInt("quantity"));
				}
			}
		}
	}

	private void updateQuantityInMarketPlace(Exchange exchange, List<DBObject> inventoryDB, String orderItemID,
			String nickNameID, int quantity) {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String orderID = exchange.getProperty("orderID", String.class);
		try {
			String apiName = "/product/stock/sellable/adjust";
			String clientID = Config.getConfig().getLazadaClientID();
			String clientSecret = Config.getConfig().getLazadaClientSecret();
			String accessToken = exchange.getProperty("accessToken", String.class);

			for (DBObject invObj : inventoryDB) {
				JSONObject inventoryObj = LazadaUtil.parseToJsonObject(invObj);
				JSONArray lazadaArray = inventoryObj.getJSONArray("lazada");
				JSONObject lazada = lazadaArray.getJSONObject(0);
				if (lazada.has("itemID") && lazada.has("skuID") && inventoryObj.has("customSKU")) {
					String payload = constructPayload(lazada.getString("itemID"), lazada.getString("skuID"),
							inventoryObj.getString("customSKU"), quantity);
					String requestBody = "payload=" + URLEncoder.encode(payload);

					JSONObject itemAmount = (JSONObject) lazada.get("itemAmount");
					String currencyCode = itemAmount.get("currencyCode").toString();
					String countryCode = LazadaUtil.currencyToCountryCodeMap.get(currencyCode);
					String url = APIUrlConfig.getNewAPIUrl(countryCode.toUpperCase());

					HashMap<String, String> map = new HashMap<String, String>();
					map.put("access_token", accessToken);
					map.put("payload", payload);
					int retryCount = 1;

					callApi(exchange, map, url, apiName, accessToken, requestBody, clientID, clientSecret,
							accountNumber, orderID, nickNameID, quantity, retryCount);
				}
			}

		} catch (Exception e) {
			log.error("Exception occurred while updating  quantity for accountNumber:" + accountNumber + "orderID:"
					+ orderID, e);
		}
	}

	private static Object callApi(Exchange exchange, HashMap<String, String> map, String url, String apiName,
			String accessToken, String requestBody, String clientID, String clientSecret, String accountNumber,
			String orderID, String nickNameID, int quantity, int retryCount) throws JSONException {

		String response = NewLazadaConnectionUtil.callAPI(url, apiName, accessToken, map, requestBody, "", "POST",
				clientID, clientSecret);
		JSONObject marketPlaceResponse = new JSONObject(response);
		boolean retry = false;

		if (marketPlaceResponse.has("code") && marketPlaceResponse.getString("code").equals("0")) {
			log.info("reserved stock released due to order moved to READT TO SHIP status for orderId:" + orderID
					+ "accountNumber:" + accountNumber + "nickNameID :" + nickNameID + "and reservedStock is :"
					+ quantity);
		} else {
			retry = true;
			log.info("Quantity updated failed for accountNumber:" + accountNumber + "orderID:" + orderID
					+ "and response is :" + response);
		}

		if (retry && retryCount <= MAX_RETRY_COUNT) {
			retryCount++;
			return callApi(exchange, map, url, apiName, accessToken, requestBody, clientID, clientSecret, accountNumber,
					orderID, nickNameID, quantity, retryCount);
		}
		return marketPlaceResponse;

	}

	private Boolean eligibleToUpdatReservedQuantities(List<DBObject> inventoryDB, String orderID, String orderItemID)
			throws JSONException {
		for (DBObject invObj : inventoryDB) {
			JSONObject inventoryObj = LazadaUtil.parseToJsonObject(invObj);
			if (inventoryObj.has("reservedQuantities")) {
				JSONArray reservedQuantitieArray = inventoryObj.getJSONArray("reservedQuantities");
				for (int i = 0; i < reservedQuantitieArray.length(); i++) {
					JSONObject reservedQuantities = reservedQuantitieArray.getJSONObject(i);
					if (reservedQuantities.has("promotionID") && reservedQuantities.get("promotionID").equals(orderID)
							&& reservedQuantities.has("orderItemID")
							&& reservedQuantities.get("orderItemID").equals(orderItemID)) {
						return false;
					}
				}
			} else {
				return true;
			}
		}
		return true;
	}

	private String constructPayload(String itemID, String skuID, String customSKU, int quantity) {
		String request = "<?xml version='1.0' encoding='UTF-8' ?>";
		request = request + "<Request><Product><Skus><Sku>";
		request = request + " <ItemId>" + itemID + "</ItemId>";
		request = request + " <SkuId>" + skuID + "</SkuId>";
		request = request + " <SellerSku><![CDATA[" + customSKU + "]]></SellerSku>";
		request = request + " <SellableQuantity>" + quantity + "</SellableQuantity>";
		request = request + "</Sku></Skus></Product></Request>";
		return request;

	}

	private String getNicknameId(DBObject userDetails) {
		BasicDBObject channel = (BasicDBObject) userDetails.get("lazada");
		BasicDBObject nickName = (BasicDBObject) channel.get("nickName");
		return nickName.getString("id");
	}

}
