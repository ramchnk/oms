package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.DateUtil;

public class InitGetSettlementDetailsRequest implements Processor {
	static Logger log = Logger.getLogger(InitGetSettlementDetailsRequest.class.getName());
	private static final int recordsPerPage = 500;

	public void process(Exchange exchange) throws Exception {
		JSONObject request = exchange.getProperty("requestObj", JSONObject.class);
		String countryCode = exchange.getProperty("countryCode", String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		int offset = 0;
		if (exchange.getProperties().containsKey("offset")) {
			offset = exchange.getProperty("offset", Integer.class);
		}
		String startDate = DateUtil.getDateFromSIAFormat(request.getLong("timeChannelSettlementStartDate"),
				"yyyy-MM-dd", LazadaUtil.timeZoneCountryMap.get(countryCode));
		String endDate = DateUtil.getDateFromSIAFormat(request.getLong("timeChannelSettlementEndDate"), "yyyy-MM-dd",
				LazadaUtil.timeZoneCountryMap.get(countryCode));

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("start_time", startDate);
		map.put("end_time", endDate);
		map.put("limit", String.valueOf(recordsPerPage));
		map.put("offset", String.valueOf(offset));

		String queryParams = "&start_time=" + URLEncoder.encode(startDate) + "&end_time=" + URLEncoder.encode(endDate)
				+ "&limit=" + recordsPerPage + "&offset=" + offset;
		callApi(queryParams, exchange, map, accessToken, offset);
	}

	private void callApi(String queryParams, Exchange exchange, HashMap<String, String> map, String accessToken,
			int offset) {
		String apiName = "/finance/transaction/details/get";
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String hostURL = exchange.getProperty("hostURL", String.class);
		exchange.setProperty("isSuccessResponse", false);
		String response = "";
		try {
			response = NewLazadaConnectionUtil.callAPI(hostURL, apiName, accessToken, map, "", queryParams, "GET",
					clientID, clientSecret);
			if (response.equals("")) {
				log.error("Getting empty response from api : " + apiName + ", for accountNumber : " + accountNumber
						+ " with nickNameID : " + nickNameID);
				exchange.setProperty("isLastPage", true);
				return;
			}
			JSONObject channelResponse = new JSONObject(response);
			if (!channelResponse.getString("code").equals("0") || !channelResponse.has("data")) {
				log.error("Getting failure response from api : " + apiName + ", for accountNumber : " + accountNumber
						+ " with nickNameID : " + nickNameID + " & response : " + channelResponse);
				exchange.setProperty("isLastPage", true);
				return;
			}
			if (channelResponse.get("data") instanceof JSONObject) {
				log.error("Getting invalid response from api : " + apiName + ", for accountNumber : " + accountNumber
						+ " with nickNameID : " + nickNameID + " & response : " + channelResponse);
				exchange.setProperty("isLastPage", true);
				return;
			}
			JSONArray data = channelResponse.getJSONArray("data");
			exchange.setProperty("isSuccessResponse", true);
			exchange.setProperty("offset", offset + recordsPerPage);
			for (int index = 0; index < data.length(); index++) {
				JSONObject settlementDetailObj = data.getJSONObject(index);
				settlementDetailObj.put("amount", settlementDetailObj.getString("amount").replace(",", ""));
				settlementDetailObj.put("WHT_amount", settlementDetailObj.getString("WHT_amount").replace(",", ""));
				settlementDetailObj.put("VAT_in_amount",
						settlementDetailObj.getString("VAT_in_amount").replace(",", ""));
			}
			exchange.setProperty("settlementDetails", data);
			if (data.length() < recordsPerPage) {
				exchange.setProperty("isLastPage", true);
			} else {
				exchange.setProperty("isLastPage", false);
			}
		} catch (Exception e) {
			log.error("Exception occurred from api : " + apiName + ", for accountNumber : " + accountNumber
					+ " with nickNameID : " + nickNameID + " and response: " + response);
			e.printStackTrace();
		}
	}

}
