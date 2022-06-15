package com.sellinall.lazada.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;

public class ProcessSettlementDetailsResponse implements Processor {

	static Logger log = Logger.getLogger(ProcessSettlementDetailsResponse.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONArray settlementDetails = exchange.getProperty("settlementDetails", JSONArray.class);
		Set<String> orderIDList = new HashSet<String>();
		for (int i = 0; i < settlementDetails.length(); i++) {
			JSONObject settlementDetailObj = settlementDetails.getJSONObject(i);
			if (settlementDetailObj.has("order_no") && !settlementDetailObj.getString("order_no").isEmpty()) {
				orderIDList.add(settlementDetailObj.getString("order_no"));
			}
		}
		checkAvailableOrders(exchange, orderIDList);
	}

	private void checkAvailableOrders(Exchange exchange, Set<String> orderIDList) throws JSONException, IOException {
		JSONArray availableOrders = getAvailableOrders(exchange, orderIDList);
		Map<String, Set<String>> orderMap = new HashMap<String, Set<String>>();
		for (int i = 0; i < availableOrders.length(); i++) {
			JSONObject resultObj = availableOrders.getJSONObject(i);
			String orderID = resultObj.getString("orderID");
			if (resultObj.getInt("httpCode") == HttpStatus.SC_OK) {
				JSONObject data = resultObj.getJSONObject("data");
				JSONArray orderItems = data.getJSONArray("orderItems");
				for (int j=0; j<orderItems.length(); j++) {
					JSONObject orderItem = orderItems.getJSONObject(j);
					Set<String> orderItemIDs = new HashSet<String>();
					if (orderMap.containsKey(orderID)) {
						orderItemIDs = orderMap.get(orderID);
					}
					orderItemIDs.add(orderItem.get("orderItemID").toString());
					orderMap.put(orderID, orderItemIDs);
				}
			}
		}
		exchange.setProperty("orderMap", orderMap);
	}

	private JSONArray getAvailableOrders(Exchange exchange, Set<String> orderIDList) throws JSONException, IOException {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		List<String> filterFields = new ArrayList<String>();
		filterFields.add("orderID");
		filterFields.add("orderItems.orderItemID");

		JSONObject requestPayload = new JSONObject();
		requestPayload.put("orderIDList", new JSONArray(orderIDList.toString()));
		requestPayload.put("nickNameID", nickNameID);
		requestPayload.put("filterFields", filterFields);

		String url = Config.getConfig().getSIAOrderURL() + "/orders/get";
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		headers.put("accountNumber", accountNumber);

		JSONObject response = HttpsURLConnectionUtil.doPost(url, requestPayload.toString(), headers);
		JSONArray availableOrders = new JSONArray();
		if (response.getInt("httpCode") == HttpStatus.SC_MULTI_STATUS) {
			JSONObject payload = new JSONObject(response.getString("payload"));
			availableOrders = payload.getJSONArray("result");
		} else {
			log.error("Failed to get order details for accountNumber : " + accountNumber + " & response : " + response);
		}
		return availableOrders;
	}

}
