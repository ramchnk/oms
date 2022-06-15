package com.sellinall.lazada.requests;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetListOfFlexiCombo implements Processor {
	static Logger log = Logger.getLogger(GetListOfFlexiCombo.class.getName());
	public long nintyDaysInSecs = 60 * 60 * 24 * 90;

	public void process(Exchange exchange) throws Exception {
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		int currentPage = exchange.getProperty("currentPage", Integer.class);
		int pageSize = exchange.getProperty("pageSize", Integer.class);
		map.put("cur_page", String.valueOf(currentPage));
		map.put("page_size", String.valueOf(pageSize));
		map.put("access_token", accessToken);
		String queryParams = "";
		queryParams += "&cur_page=" + currentPage;
		queryParams += "&page_size=" + pageSize;
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = "";
		try {
			response = NewLazadaConnectionUtil.callAPI(hostURL, "/promotion/flexicombo/list", accessToken, map, "",
					queryParams, "GET", clientID, clientSecret);
			if (response.isEmpty()) {
				exchange.setProperty("hasMoreRecordList", false);
				log.error("Failed to get list of flexi combo for accountNumber :"
						+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
						+ exchange.getProperty("nickNameID", String.class) + " and response is" + response.toString());
				exchange.setProperty("failureReason", "Getting empty response from lazada, please try again" );
				return;
			}
			JSONObject serviceResponse = new JSONObject(response);
			if (serviceResponse.has("code") && serviceResponse.getString("code").equals("0")) {
				if (serviceResponse.has("data")) {
					getFlexiComboList(serviceResponse.getJSONObject("data"), exchange);
					controlGetListPagination(serviceResponse.getJSONObject("data"), exchange);
				}
				return;
			}
			exchange.setProperty("hasMoreRecordList", false);
			log.error("Failed to get list of flexi combo for accountNumber :"
					+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response is" + response.toString());
			exchange.setProperty("failureReason", "unable to import" + response.toString());
			if (serviceResponse.has("message")) {
				exchange.setProperty("failureReason", serviceResponse.getString("message"));
			}
		} catch (Exception e) {
			exchange.setProperty("hasMoreRecordList", false);
			log.error("Exception ocurred while get list of flexi combo for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response is " + response.toString(), e);
			exchange.setProperty("failureReason", "unable to import" + e.getMessage());
		}
	}

	public static void getFlexiComboList(JSONObject data, Exchange exchange) throws JSONException {
		ArrayList<JSONObject> flexiComboIdList = new ArrayList<JSONObject>();
		if (data.has("data_list")) {
			JSONArray dataList = data.getJSONArray("data_list");
			for (int i = 0; i < dataList.length(); i++) {
				flexiComboIdList.add(dataList.getJSONObject(i));
				updateFlexiComboImportCount(exchange);
			}
		}
		exchange.getOut().setBody(flexiComboIdList);
	}

	public static void updateFlexiComboImportCount(Exchange exchange) {
		int currentCount = exchange.getProperty("totalNumberOfPromotions", Integer.class);
		currentCount += 1;
		exchange.setProperty("totalNumberOfPromotions", currentCount);
	}

	public void controlGetListPagination(JSONObject data, Exchange exchange)
			throws NumberFormatException, JSONException {
		int total = Integer.parseInt(data.getString("total"));
		int currentPage = exchange.getProperty("currentPage", Integer.class);
		int pageSize = Integer.parseInt(data.getString("page_size"));
		if (total <= pageSize) {
			exchange.setProperty("hasMoreRecordList", false);
			return;
		}
		double totalpageNumber = Math.ceil(total * 1.0 / pageSize * 1.0);
		long lastItemStartDate = 0;
		if (data.has("data_list")) {
			JSONArray dataList = data.getJSONArray("data_list");
			int dataListLength = (dataList.length() > 0) ? dataList.length() - 1 : 0;
			JSONObject dataListObject = dataList.getJSONObject(dataListLength);
			lastItemStartDate = dataListObject.getLong("start_time") / 1000;
		}
		long currentTime = System.currentTimeMillis() / 1000;
		long nintyDaysBack = currentTime - nintyDaysInSecs;
		if (totalpageNumber <= currentPage || nintyDaysBack > lastItemStartDate) {
			exchange.setProperty("hasMoreRecordList", false);
		}
		currentPage += 1;
		exchange.setProperty("currentPage", currentPage);
	}
}
