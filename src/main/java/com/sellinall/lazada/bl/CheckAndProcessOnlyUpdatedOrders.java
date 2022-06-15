package com.sellinall.lazada.bl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.DateUtil;
import com.sellinall.util.HttpsURLConnectionUtil;

public class CheckAndProcessOnlyUpdatedOrders implements Processor {

	static Logger log = Logger.getLogger(CheckAndProcessOnlyUpdatedOrders.class.getName());

	public void process(Exchange exchange) throws Exception {
		ArrayList<JSONObject> arrayList = (ArrayList<JSONObject>) exchange.getIn().getBody();
		getOrderIDListToProcess(exchange, arrayList);
		exchange.getOut().setBody(arrayList);
	}

	private void getOrderIDListToProcess(Exchange exchange, ArrayList<JSONObject> arrayList)
			throws JSONException, IOException {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String countryCode = exchange.getProperty("countryCode", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String requestType = exchange.getProperty("requestType", String.class);
		List<String> newOrderIDList = new ArrayList<String>();
		List<String> orderIDList = new ArrayList<String>();
		for (JSONObject order : arrayList) {
			orderIDList.add(order.getString("order_id"));
		}
		List<String> filterFields = Arrays.asList("orderID", "timeOrderUpdated");

		JSONObject payload = new JSONObject();
		payload.put("orderIDList", orderIDList);
		payload.put("nickNameID", nickNameID);
		payload.put("filterFields", filterFields);

		JSONObject response = getAvailableOrders(accountNumber, payload);
		Map<String, Long> availableOrderIDMap = getAvailableOrderIDList(response, newOrderIDList);
		filterOrdersToProcess(arrayList, availableOrderIDMap, newOrderIDList, accountNumber, nickNameID, countryCode,
				orderIDList, requestType);
	}

	private void filterOrdersToProcess(ArrayList<JSONObject> arrayList, Map<String, Long> availableOrderIDMap,
			List<String> newOrderIDList, String accountNumber, String nickNameID, String countryCode,
			List<String> orderIDList, String requestType) throws JSONException {
		List<String> removeOrderIDList = new ArrayList<String>();
		for (int i = 0; i < arrayList.size();) {
			JSONObject order = arrayList.get(i);
			String orderID = order.getString("order_id");
			if (newOrderIDList.contains(orderID)) {
				i++;
			} else if (availableOrderIDMap.containsKey(orderID)) {
				String updatedTime = order.getString("updated_at").substring(0, 19);
				long orderUpdatedTime = DateUtil.getUnixTimestamp(updatedTime, "yyyy-MM-dd HH:mm:ss",
						LazadaUtil.timeZoneCountryMap.get(countryCode));
				long orderUpdatedTimeFromDB = availableOrderIDMap.get(orderID);
				if (orderUpdatedTime > orderUpdatedTimeFromDB) {
					i++;
				} else {
					removeOrderIDList.add(orderID);
					arrayList.remove(i);
				}
			} else {
				removeOrderIDList.add(orderID);
				arrayList.remove(i);
			}
		}
		if (!removeOrderIDList.isEmpty()) {
			log.info("No updates found for following orderIDs, so skipping these orderIDs : " + removeOrderIDList
					+ ", for accountNumber : " + accountNumber + " , nickNameID : " + nickNameID + ", requestType : "
					+ requestType);
		}
		orderIDList.removeAll(removeOrderIDList);
		log.info("Currently processing orderIDs : " + orderIDList + ", for accountNumber : " + accountNumber
				+ " , nickNameID : " + nickNameID + ", requestType : " + requestType);
	}

	private Map<String, Long> getAvailableOrderIDList(JSONObject response, List<String> newOrderIDList)
			throws JSONException {
		Map<String, Long> map = new HashMap<String, Long>();
		if (response.getInt("httpCode") == HttpStatus.SC_MULTI_STATUS) {
			JSONObject payload = new JSONObject(response.getString("payload"));
			JSONArray result = payload.getJSONArray("result");
			for (int i = 0; i < result.length(); i++) {
				JSONObject orderObj = result.getJSONObject(i);
				if (orderObj.getInt("httpCode") == HttpStatus.SC_OK) {
					JSONObject dataObj = orderObj.getJSONObject("data");
					if (dataObj.has("timeOrderUpdated")) {
						map.put(dataObj.getString("orderID"), dataObj.getLong("timeOrderUpdated"));
					} else {
						/*
						 * Note: if timeOrderUpdated is not present in our side, then will consider this
						 * as new order, so it will get processed
						 */
						newOrderIDList.add(orderObj.getString("orderID"));
					}
				} else if (orderObj.getInt("httpCode") == HttpStatus.SC_NOT_FOUND) {
					newOrderIDList.add(orderObj.getString("orderID"));
				}
			}
		}
		return map;
	}

	private JSONObject getAvailableOrders(String accountNumber, JSONObject payload) throws IOException, JSONException {
		String url = Config.getConfig().getSIAOrderURL() + "/orders/get";
		Map<String, String> map = new HashMap<String, String>();
		map.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		map.put("accountNumber", accountNumber);
		return HttpsURLConnectionUtil.doPost(url, payload.toString(), map);
	}

}
