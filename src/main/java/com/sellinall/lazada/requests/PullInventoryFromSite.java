package com.sellinall.lazada.requests;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.DateUtil;

public class PullInventoryFromSite implements Processor {
	static Logger log = Logger.getLogger(PullInventoryFromSite.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accountNumber = "";
		if (exchange.getProperties().containsKey("accountNumber")) {
			accountNumber = (String) exchange.getProperty("accountNumber");
		}
		exchange.setProperty("responseSuccess", false);
		String response = "";
		ArrayList<JSONObject> arrayList = new ArrayList<JSONObject>();
		JSONArray products = new JSONArray();
		exchange.setProperty("isLastLoop", false);
		int offset = 0;
		String pullInventoryType = exchange.getIn().getHeader("pullInventoryType", String.class);
		String requestType = "";
		if (exchange.getProperties().containsKey("requestType")) {
			requestType = exchange.getProperty("requestType", String.class);
		}
		exchange.setProperty("pullInventoryType", pullInventoryType);
		int limit = Config.getConfig().getRecordsPerPage();
		if (exchange.getProperties().containsKey("importType")
				&& exchange.getProperty("importType", String.class).equals("noOfRecords")
				&& exchange.getProperties().containsKey("importRecordsLimit")) {
			limit = exchange.getProperty("importRecordsLimit", Integer.class);
			int numberOfRecords = exchange.getProperty("numberOfRecords", Integer.class);
			int numberOfPages = (numberOfRecords / limit) + ((numberOfRecords % limit > 0) ? 1 : 0);
			exchange.setProperty("numberOfPages", numberOfPages);
		}
		if (pullInventoryType.equals("POLLING") && !requestType.equals("processPendingNotification")) {
			String lastScannedTime = exchange.getProperty("lastScannedTime", String.class);
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("create_after", lastScannedTime.replace("Z", ""));
			products = callApi(params, exchange, "", "", lastScannedTime, accountNumber);
		} else if (exchange.getProperties().containsKey("importType")
				&& exchange.getProperty("importType", String.class).equals("dateRange")) {	
			HashMap<String, String> params = new HashMap<String, String>();
			if (exchange.getProperties().containsKey("fromDate")) {
				String fromDate = DateUtil.getDateFromSIAFormat(exchange.getProperty("fromDate", Long.class),
						"yyyy-MM-dd'T'HH:mm:ssXXX",
						LazadaUtil.timeZoneCountryMap.get(exchange.getProperty("countryCode", String.class)));
				params.put("update_after", fromDate);
			}
			if (exchange.getProperties().containsKey("toDate")) {
				String toDate = DateUtil.getDateFromSIAFormat(exchange.getProperty("toDate", Long.class),
						"yyyy-MM-dd'T'HH:mm:ssXXX",
						LazadaUtil.timeZoneCountryMap.get(exchange.getProperty("countryCode", String.class)));
				params.put("update_before", toDate);
			}
			offset = exchange.getProperty("offset", Integer.class);
			products = callApi(params, exchange, "" + limit, "" + offset, "", accountNumber);
		} else {
			offset = exchange.getProperty("offset", Integer.class);
			products = callApi(new HashMap<String, String>(), exchange, "" + limit, "" + offset, "", accountNumber);
		}
		int totalChildItemEntries = 0;
		if (exchange.getProperties().containsKey("totalChildItemEntries")) {
			totalChildItemEntries = exchange.getProperty("totalChildItemEntries", Integer.class);
		}
		if (products == null) {
			return;
		} else {
			exchange.setProperty("totalChildItemEntries", totalChildItemEntries + products.length());
		}

		int totalNoOfRecord = 0;
		if (exchange.getProperties().containsKey("numberOfRecords")) {
			totalNoOfRecord = exchange.getProperty("numberOfRecords", Integer.class);
		}
		Set<String> itemIDList = new HashSet<String>();
		if(exchange.getProperties().containsKey("itemIDList")) {
			itemIDList = exchange.getProperty("itemIDList", Set.class);
		}
		int processedItemSize = itemIDList.size();
		for (int i = 0; i < products.length(); i++) {
			JSONObject product = products.getJSONObject(i);
			if (totalNoOfRecord == processedItemSize && exchange.getProperties().containsKey("importType")
					&& !exchange.getProperty("importType", String.class).equals("dateRange")) {
				break;
			}
			if (!itemIDList.contains(product.getString("item_id"))) {
				itemIDList.add(product.getString("item_id"));
				arrayList.add(product);
				processedItemSize++;
			}
		}
		exchange.setProperty("itemIDList", itemIDList);
		exchange.setProperty("offset", offset + products.length());
		exchange.setProperty("isInventoryPollingSuccess", true);
		if (!exchange.getProperty("stopRetryOnServiceTimeout", Boolean.class)) {
			exchange.setProperty("responseSuccess", true);
		}
		exchange.getOut().setBody(arrayList);
		int totalEntries = 0;
		if (exchange.getProperties().containsKey("totalEntries")) {
			totalEntries = exchange.getProperty("totalEntries", Integer.class);
		}
		totalEntries = totalEntries + arrayList.size();
		exchange.setProperty("totalEntries", totalEntries);
		if ((totalEntries == totalNoOfRecord || products.length() != limit) && exchange.getProperties().containsKey("importType")
				&& exchange.getProperty("importType", String.class).equals("noOfRecords")) {
			exchange.setProperty("isLastLoop", true);
		}
		if (arrayList.size() < limit && exchange.getProperties().containsKey("importType")
				&& exchange.getProperty("importType", String.class).equals("dateRange")) {
			exchange.setProperty("isLastLoop", true);
		}
		exchange.setProperty("pulledInventoryList", arrayList);
		exchange.setProperty("totalItemsInCurrentPage", arrayList.size());
		exchange.setProperty("itemListIndex", 0);
		log.debug("Response From Channel = " + response + " for account : " + accountNumber); // print out the XML
	}

	private static JSONArray callApi(HashMap<String, String> map, Exchange exchange, String limit, String offset,
			String lastScannedTime, String accountNumber) throws IOException {
		JSONArray products = new JSONArray();
		String response = "";
		String queryParam = "";
		String accessToken = exchange.getProperty("accessToken", String.class);
		String ScApiHost = exchange.getProperty("hostURL", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		map.put("access_token", accessToken);
		if (!lastScannedTime.isEmpty()) {
			queryParam += "&create_after=" + URLEncoder.encode(lastScannedTime.replace("Z", ""));
		}
		String filter = "all";
		if(exchange.getProperties().containsKey("importStatusFilter")) {
			filter = exchange.getProperty("importStatusFilter", String.class);
		}
		if (map.containsKey("update_after") && map.containsKey("update_before")){
			queryParam += "&update_after=" + URLEncoder.encode(map.get("update_after"));
			queryParam += "&update_before=" + URLEncoder.encode(map.get("update_before"));
		}
		map.put("filter", filter);
		queryParam += "&filter=" + filter;
		if (!limit.isEmpty() && !offset.isEmpty()) {
			map.put("limit", "" + limit);
			map.put("offset", "" + offset);
			queryParam += "&limit=" + limit + "&offset=" + offset;
		}
		String apiName = "/products/get";
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		try {
			response = NewLazadaConnectionUtil.callAPI(ScApiHost, apiName, accessToken, map, "", queryParam, "GET",
					clientID, clientSecret);
			if (response.equals("")) {
				log.error("Getting empty response from api : " + apiName + ", for accountNumber : " + accountNumber
						+ " with nickNameID : " + nickNameID);
				// communication failure (error message already printed)
				exchange.setProperty("isInventoryPollingSuccess", false);
				return null;
			}
			// response
			JSONObject channelResponse = new JSONObject(response);
			if (!channelResponse.has("data")) {
				exchange.setProperty("isInventoryPollingSuccess", false);
				checkForRetry(channelResponse, exchange, accountNumber, nickNameID);
				return null;
			}
			if (checkForRetry(channelResponse, exchange, accountNumber, nickNameID)) {
				return null;
			}
			JSONObject data = channelResponse.getJSONObject("data");
			if (data.has("total_products")) {
				exchange.setProperty("totalProducts", Integer.parseInt(data.getString("total_products")));
			}
			if (!data.has("products") || data.get("products") instanceof String
					|| (data.get("products") instanceof JSONArray && data.getJSONArray("products").length() == 0)) {
				// response doesn't contains any items
				exchange.setProperty("isLastLoop", true);
				exchange.setProperty("isInventoryPollingSuccess", true);
				log.debug("response doesn't contains any items" + " for account : " + accountNumber);
				return null;
			}
			products = data.getJSONArray("products");

		} catch (Exception e) {
			log.error("No item Found, for account : " + accountNumber + " and nickNameId: "
					+ exchange.getProperty("nickNameID", String.class) + " and response: " + response);
			e.printStackTrace();
		}
		return products;
	}

	private static boolean checkForRetry(JSONObject channelResponse, Exchange exchange, String accountNumber,
			String nickNameID) throws JSONException {
		boolean isRetry = false;
		boolean stopRetryOnServiceTimeout = false;
		if (channelResponse.getString("code").equals("ServiceTimeout")) {
			isRetry = true;
		} else if (channelResponse.getString("code").contains("ApiCallLimit")) {
			/* TODO: will check and put delay later */
			isRetry = true;
		} else if (channelResponse.has("data") && channelResponse.getJSONObject("data").length() == 0) {
			if ((exchange.getProperties().containsKey("totalProducts")
					&& exchange.getProperties().containsKey("totalEntries")
					&& exchange.getProperty("totalEntries", Integer.class) >= exchange.getProperty("totalProducts",
							Integer.class))
					|| (exchange.getProperties().containsKey("totalProducts")
							&& exchange.getProperties().containsKey("totalChildItemEntries")
							&& exchange.getProperty("totalChildItemEntries", Integer.class) == exchange
									.getProperty("totalProducts", Integer.class))) {
				exchange.setProperty("isLastLoop", true);
				exchange.setProperty("isInventoryPollingSuccess", true);
				isRetry = false;
			} else {
				isRetry = true;
			}
		}
		exchange.setProperty("isRetry", isRetry);
		if (isRetry) {
			log.error("Got error response for accountNumber : " + accountNumber + ", nickNameID : " + nickNameID
					+ " and response : " + channelResponse);
			JSONObject currentPageMessage = new JSONObject();
			if (exchange.getProperties().containsKey("currentPageMessage")) {
				// Retrying service time-out crossed after first page.
				currentPageMessage = exchange.getProperty("currentPageMessage", JSONObject.class);
			} else {
				// First page service time-out handled here.
				currentPageMessage = exchange.getProperty("initialRetryMessage", JSONObject.class);
			}
			if (currentPageMessage.getInt("retryCount") < 3) {
				exchange.setProperty("retryLimitExceeded", false);
				int incrementRetryCount = currentPageMessage.getInt("retryCount") + 1;
				currentPageMessage.put("retryCount", incrementRetryCount);
			} else {
				exchange.setProperty("retryLimitExceeded", true);
				if (channelResponse.getString("code").equals("ServiceTimeout")) {
					stopRetryOnServiceTimeout = true;
					exchange.setProperty("failureReason", "Lazada API Error: " + channelResponse.getString("message")
							+ ", Please try again after 15-30 mins");
				} else if (channelResponse.getString("code").equals("0")) {
					stopRetryOnServiceTimeout = true;
					exchange.setProperty("failureReason", "Lazada API failed so please try again after 15-30 mins");
				}
			}
		}
		exchange.setProperty("stopRetryOnServiceTimeout", stopRetryOnServiceTimeout);
		return isRetry;
	}
}