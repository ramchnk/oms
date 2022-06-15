package com.sellinall.lazada.requests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetAvailableVoucherList implements Processor {
	static Logger log = Logger.getLogger(GetAvailableVoucherList.class.getName());

	public void process(Exchange exchange) throws Exception {

		int pageNumber = exchange.getProperty("pageNumber", Integer.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("voucher_type", exchange.getProperty("voucherType", String.class));
		map.put("cur_page", String.valueOf(pageNumber));
		map.put("page_size", String.valueOf(Config.getConfig().getRecordsPerPage()));
		String param = "&voucher_type=" + exchange.getProperty("voucherType", String.class);
		param += "&cur_page=" + pageNumber;
		param += "&page_size=" + Config.getConfig().getRecordsPerPage();
		int retryCount = 1;
		List<JSONObject> response = callAPI(exchange, map, param, retryCount);
		exchange.getOut().setBody(response);
	}

	private List<JSONObject> callAPI(Exchange exchange, HashMap<String, String> map, String queryParams, int retryCount)
			throws JSONException, InterruptedException {
		String accessToken = exchange.getProperty("accessToken", String.class);
		String url = exchange.getProperty("hostURL", String.class);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = NewLazadaConnectionUtil.callAPI(url, "/promotion/vouchers/get", accessToken, map, "",
				queryParams, "GET", clientID, clientSecret);
		if (response.isEmpty() || !response.startsWith("{")) {
			exchange.setProperty("isLastPage", true);
			log.error("Invalid voucher response getting for accountNumber: " + exchange.getProperty("accountNumber")
					+ ", nickNameId: " + exchange.getProperty("nickNameID") + " and the response is: " + response);
			return new ArrayList<JSONObject>();
		}
		JSONObject responseObject = new JSONObject(response);
		if (!responseObject.getString("code").equals("0")) {
			exchange.setProperty("isLastPage", true);
			log.error("Error occuring for accountNumber : " + exchange.getProperty("accountNumber") + ", nickNameID : "
					+ exchange.getProperty("nickNameID") + " and response is: " + response);
			return new ArrayList<JSONObject>();
		}
		JSONObject data = responseObject.getJSONObject("data");
		if (data.has("total")) {
			exchange.setProperty("totalVoucher", Integer.parseInt(data.getString("total")));
		}
		if (!data.has("data_list")) {
			log.error("Invalid voucher response getting for accountNumber : " + exchange.getProperty("accountNumber")
					+ ", nickNameID : " + exchange.getProperty("nickNameID") + ", retryCount : " + retryCount
					+ " & response : " + response);
			if (retryCount <= 3) {
				retryCount++;
				Thread.sleep(1000);
				return callAPI(exchange, map, queryParams, retryCount);
			}
			return new ArrayList<JSONObject>();
		}
		ArrayList<JSONObject> voucherList = new ArrayList<JSONObject>();
		if (data.has("data_list")) {
			exchange.setProperty("isLastPage", false);
			JSONArray dataList = data.getJSONArray("data_list");
			for (int i = 0; i < dataList.length(); i++) {
				JSONObject dataObj = dataList.getJSONObject(i);
				if (!dataObj.getString("status").equalsIgnoreCase("FINISH")) {
					voucherList.add(dataObj);
				}
			}
		}
		return voucherList;
	}

}
