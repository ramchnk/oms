package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.mongodb.BasicDBObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.DateUtil;

public class PullReconciledFromSite implements Processor {
	static Logger log = Logger.getLogger(PullReconciledFromSite.class.getName());

	public void process(Exchange exchange) throws Exception {
		String startDate = parseStartTime(exchange);
		String endDate = parseEndTime();
		JSONArray transactionDOs = callApi(exchange,startDate,endDate);
		if(transactionDOs == null) {
			return;
		}
		exchange.setProperty("reconcileHasData", true);
		exchange.setProperty("orderList", transactionDOs);
		exchange.setProperty("totalEntries", transactionDOs.length()-1);
		exchange.setProperty("orderIndex", 0);
		exchange.setProperty("isLastRow", false);
	}

	private JSONArray callApi(Exchange exchange, String startDate, String endDate) {
		String accountNumber = "";
		if (exchange.getProperties().containsKey("accountNumber")) {
			accountNumber = (String) exchange.getProperty("accountNumber");
		}
		HashMap<String, String> map = new HashMap<String, String>();
		String response = "";
		String queryParam = "";
		String accessToken = exchange.getProperty("accessToken", String.class);
		String ScApiHost = exchange.getProperty("hostURL", String.class);
		map.put("access_token", accessToken);
		map.put("start_time", URLEncoder.encode(startDate));
		map.put("end_time", URLEncoder.encode(endDate));
		map.put("limit", "500");
		map.put("offset", "0");
		queryParam += "&start_time=" + URLEncoder.encode(startDate) + "&end_time=" + URLEncoder.encode(endDate)
				+ "&limit=500&offset=0";
		String apiName = "/finance/transaction/detail/get";
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		try {
			response = NewLazadaConnectionUtil.callAPI(ScApiHost, apiName, accessToken, map, "", queryParam, "GET",
					clientID, clientSecret);
			// response
			JSONObject channelResponse = new JSONObject(response);
			if (!channelResponse.has("data")) {
				log.error(response + ", for account :" + accountNumber);
				return null;
			}
			JSONArray data = channelResponse.getJSONArray("data");
			if (data.length() == 0) {
				log.info("There is no transaction on " + startDate + " " + endDate + " this period");
				return null;
			}
			return data;

		} catch (Exception e) {
			log.error("No item Found, for account : " + accountNumber + " and nickNameId: "
					+ exchange.getProperty("nickNameID", String.class) + " and response: " + response);
			e.printStackTrace();
		}
		return null;
	}

	private String parseStartTime(Exchange exchange) {
		BasicDBObject channel = exchange.getProperty("channel", BasicDBObject.class);
		String startTime = "";
		if (channel.containsField("lastReconciledDate")) {
			startTime = DateUtil.getDateFromSIAFormat(channel.getLong("lastReconciledDate"), "YYYY-MM-DD", "UTC");
		} else {
			DateTime currentDate = new DateTime().withZone(DateTimeZone.UTC);
			currentDate = currentDate.minusDays(15);
			startTime = currentDate.getYear() + "-" + currentDate.getMonthOfYear() + "-" + currentDate.getDayOfMonth();
		}
		return startTime;
	}

	private String parseEndTime() {
		DateTime currentDate = new DateTime().withZone(DateTimeZone.UTC);
		String endTime = currentDate.getYear() + "-" + currentDate.getMonthOfYear() + "-" + currentDate.getDayOfMonth();
		return endTime;
	}
}