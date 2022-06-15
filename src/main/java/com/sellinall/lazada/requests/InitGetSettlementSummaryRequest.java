package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Weeks;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.DateUtil;

public class InitGetSettlementSummaryRequest implements Processor {
	static Logger log = Logger.getLogger(InitGetSettlementSummaryRequest.class.getName());
	public static final long ONE_DAY = 86400;

	public void process(Exchange exchange) throws Exception {
		JSONObject request = exchange.getProperty("requestObj", JSONObject.class);
		exchange.setProperty("isSuccessResponse", false);
		callAPI(exchange, request, 1);
	}

	private void callAPI(Exchange exchange, JSONObject request, int retryCount) throws JSONException {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String hostURL = exchange.getProperty("hostURL", String.class);
		String countryCode = exchange.getProperty("countryCode", String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String apiName = "/finance/payout/status/get";

		/*
		 * if selected date is Monday, then we will use that date, else we will
		 * process with previous Monday date
		 */
		String startPeriod = LazadaUtil.getPreviousDate(request.getLong("timeChannelSettlementStartDate"),
				DateTimeConstants.MONDAY, countryCode);
		String endPeriod = LazadaUtil.getPreviousDate(request.getLong("timeChannelSettlementEndDate"),
				DateTimeConstants.SUNDAY, countryCode);

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("created_after", startPeriod);
		String queryParams = "&created_after=" + URLEncoder.encode(startPeriod);

		String response = "";
		try {
			response = NewLazadaConnectionUtil.callAPI(hostURL, apiName, accessToken, map, "", queryParams, "GET",
					clientID, clientSecret);
			boolean isNeedToRetryApiCall = false, isSuccessResponse = true;
			if (response.equals("")) {
				log.error("Getting empty response from api : " + apiName + ", for accountNumber : " + accountNumber
						+ " with nickNameID : " + nickNameID);
				isNeedToRetryApiCall = true;
				isSuccessResponse = false;
			}
			JSONObject channelResponse = new JSONObject(response);
			if (!channelResponse.getString("code").equals("0") || !channelResponse.has("data")
					|| channelResponse.get("data") instanceof JSONObject) {
				log.error("Getting failure response from api : " + apiName + ", for accountNumber : " + accountNumber
						+ " with nickNameID : " + nickNameID + " & response : " + response);
				isNeedToRetryApiCall = true;
				isSuccessResponse = false;
			}
			if (isSuccessResponse) {
				exchange.setProperty("isSuccessResponse", isSuccessResponse);
				exchange.setProperty("settlementSummary", processSettlementSummary(channelResponse.getJSONArray("data"),
						countryCode, startPeriod, endPeriod,exchange));
			} else if (isNeedToRetryApiCall && retryCount < 3) {
				retryCount = retryCount + 1;
				callAPI(exchange, request, retryCount);
			}
		} catch (Exception e) {
			log.error("Exception occurred from api : " + apiName + ", for accountNumber : " + accountNumber
					+ " with nickNameID : " + nickNameID + " and response: " + response);
			e.printStackTrace();
		}
	}

	private JSONArray processSettlementSummary(JSONArray data, String countryCode, String startPeriod, String endPeriod, Exchange exchange)
			throws JSONException, ParseException {
		Map<String, JSONObject> periodOfWeeksMap = getPeriodOfWeeksMap(startPeriod, endPeriod, countryCode);
		List<JSONObject> dataList = new ArrayList<JSONObject>();
		Map<String, List<JSONObject>> periodWiseSummaryMap = separateDataList(data, countryCode);
		for(Entry<String, List<JSONObject>> entrySet : periodWiseSummaryMap.entrySet()) {
			List<JSONObject> samePeriodSummaryList = entrySet.getValue();
			if (samePeriodSummaryList.size() > 1) {
				processDayWiseSummary(samePeriodSummaryList, dataList, countryCode, periodOfWeeksMap);
			} else {
				JSONObject summaryObj = samePeriodSummaryList.get(0);
				SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
				JSONArray settlementDetails;
				String settlementStartDate;
				Date createdDate = sdf1.parse(summaryObj.getString("created_at").split(" ")[0]);
				//get previous day to get the settlement details
				settlementStartDate = sdf1.format(new Date(createdDate.getTime() - 24 * 3600 * 1000));
				settlementDetails = getSettlementDetailsResponse(exchange,settlementStartDate,settlementStartDate);
				if(isDayWiseSettlement(settlementDetails)) {
					processDayWiseSummary(samePeriodSummaryList, dataList, countryCode, periodOfWeeksMap);
					continue;
				}
				//get 7 days settlement details
				settlementDetails = getSettlementDetailsResponse(exchange,sdf1.format(new Date(createdDate.getTime() - 7 * 24 * 3600 * 1000)),settlementStartDate);
				if(isDayWiseSettlement(settlementDetails)) {
					processDayWiseSummary(samePeriodSummaryList, dataList, countryCode, periodOfWeeksMap);
					continue;
				}
				processWeeklySummary(summaryObj, dataList, countryCode, periodOfWeeksMap);
			}
		}

		for (String key : periodOfWeeksMap.keySet()) {
			JSONObject pendingObj = periodOfWeeksMap.get(key);
			dataList.add(pendingObj);
		}
		return new JSONArray(dataList.toString());
	}

	private boolean isDayWiseSettlement(JSONArray settlementDetails) throws JSONException, ParseException {
		if (settlementDetails.length() > 0) {
			String statement = settlementDetails.getJSONObject(0).getString("statement");
			SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
			Date startDate = sdf.parse(statement.split("-")[0]);
			Date endDate = sdf.parse(statement.split("-")[1]);
			if (startDate.compareTo(endDate) == 0) {
				return true;
			}
		}
		return false;
	}

	private void processWeeklySummary(JSONObject summaryObj, List<JSONObject> dataList, String countryCode,
			Map<String, JSONObject> periodOfWeeksMap) throws JSONException {
		int defaultSettlementDateRange = Config.getConfig().getDefaultSettlementDateRange(countryCode);
		long settlementStartDate = LazadaUtil.getUnixTimeWithTimeZone(summaryObj.getString("created_at").split(" ")[0],
				"yyyy-MM-dd", countryCode);
		String strSettlementStartDate = LazadaUtil.getPreviousDate(settlementStartDate, DateTimeConstants.MONDAY,
				countryCode);
		long startUnixTimestamp = LazadaUtil.getUnixTimeWithTimeZone(strSettlementStartDate, "yyyy-MM-dd", countryCode);
		/*
		 * settlement date range is last week of settled date, so getting settlement
		 * start date and end date from settled date
		 */
		summaryObj.put("timeChannelSettlementStartDate", startUnixTimestamp - (defaultSettlementDateRange * ONE_DAY));
		summaryObj.put("timeChannelSettlementEndDate", startUnixTimestamp - 1);
		summaryObj.put("transactionPeriod",
				DateUtil.getDateFromSIAFormat((startUnixTimestamp - (defaultSettlementDateRange * ONE_DAY)),
						"dd MMM yyyy", LazadaUtil.timeZoneCountryMap.get(countryCode)) + " - "
						+ DateUtil.getDateFromSIAFormat((startUnixTimestamp - 1), "dd MMM yyyy",
								LazadaUtil.timeZoneCountryMap.get(countryCode)));
		if (periodOfWeeksMap.containsKey(summaryObj.get("transactionPeriod"))) {
			periodOfWeeksMap.remove(summaryObj.get("transactionPeriod"));
			dataList.add(summaryObj);
		}

	}

	private void processDayWiseSummary(List<JSONObject> samePeriodSummaryList, List<JSONObject> dataList,
			String countryCode, Map<String, JSONObject> periodOfWeeksMap) throws JSONException {
		JSONObject summaryObj = new JSONObject();
		int defaultSettlementDateRange = Config.getConfig().getDefaultSettlementDateRange(countryCode);
		Map<String, String> totalAmountMap = new HashMap<String, String>();
		List<String> nonAmountField = new ArrayList<String>(
				Arrays.asList("created_at", "updated_at", "statement_number", "timeChannelSettlementStartDate",
						"timeChannelSettlementEndDate", "transactionPeriod", "paid"));
		for (JSONObject data : samePeriodSummaryList) {
			Iterator keys = data.keys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				if (!nonAmountField.contains(key)) {
					String stringValue = !data.getString(key).equals("") ? data.getString(key) : "0";
					String stringTotalFromMap = totalAmountMap.containsKey(key) ? totalAmountMap.get(key) : "0";
					String total = String.valueOf(Double.parseDouble(stringTotalFromMap)
							+ Double.parseDouble(stringValue.replaceAll("[a-zA-Z]", "")));
					totalAmountMap.put(key, total);
				}
			}
		}
		for (Entry<String, String> totalAmountEntry : totalAmountMap.entrySet()) {
			summaryObj.put(totalAmountEntry.getKey(), totalAmountEntry.getValue());
		}
		summaryObj.put("settlementSummaryList", samePeriodSummaryList);
		JSONObject daySummaryObj = samePeriodSummaryList.get(0);
		summaryObj.put("paid", daySummaryObj.get("paid"));
		long settlementStartDate = LazadaUtil.getUnixTimeWithTimeZone(
				daySummaryObj.getString("created_at").split(" ")[0], "yyyy-MM-dd", countryCode);
		String strSettlementStartDate = LazadaUtil.getPreviousDate(settlementStartDate, DateTimeConstants.MONDAY,
				countryCode);
		long startUnixTimestamp = LazadaUtil.getUnixTimeWithTimeZone(strSettlementStartDate, "yyyy-MM-dd", countryCode);
		long endUnixTimestamp = LazadaUtil.getUnixTimeWithTimeZone(strSettlementStartDate, "yyyy-MM-dd", countryCode)
				+ (defaultSettlementDateRange * ONE_DAY) - 1;

		summaryObj.put("timeChannelSettlementStartDate", startUnixTimestamp);
		summaryObj.put("timeChannelSettlementEndDate", endUnixTimestamp);
		summaryObj.put("transactionPeriod",
				DateUtil.getDateFromSIAFormat((startUnixTimestamp), "dd MMM yyyy",
						LazadaUtil.timeZoneCountryMap.get(countryCode)) + " - "
						+ DateUtil.getDateFromSIAFormat((endUnixTimestamp), "dd MMM yyyy",
								LazadaUtil.timeZoneCountryMap.get(countryCode)));
		if (periodOfWeeksMap.containsKey(summaryObj.get("transactionPeriod"))) {
			periodOfWeeksMap.remove(summaryObj.get("transactionPeriod"));
			dataList.add(summaryObj);
		} else {
			log.error(summaryObj.get("transactionPeriod") + "this summary creation error occured");
		}
	}

	private JSONArray getSettlementDetailsResponse(Exchange exchange, String startDate, String endDate)
			throws JSONException {
		JSONArray data = new JSONArray();
		String accessToken = exchange.getProperty("accessToken", String.class);
		int offset = 0;
		if (exchange.getProperties().containsKey("offset")) {
			offset = exchange.getProperty("offset", Integer.class);
		}

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("start_time", startDate);
		map.put("end_time", endDate);
		map.put("limit", String.valueOf(10));
		map.put("offset", String.valueOf(offset));

		String queryParams = "&start_time=" + URLEncoder.encode(startDate) + "&end_time=" + URLEncoder.encode(endDate)
				+ "&limit=" + 10 + "&offset=" + offset;

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
				return data;
			}
			JSONObject channelResponse = new JSONObject(response);
			if (!channelResponse.getString("code").equals("0") || !channelResponse.has("data")) {
				log.error("Getting failure response from api : " + apiName + ", for accountNumber : " + accountNumber
						+ " with nickNameID : " + nickNameID + " & response : " + channelResponse);
				return data;
			}
			if (channelResponse.get("data") instanceof JSONObject) {
				log.error("Getting invalid response from api : " + apiName + ", for accountNumber : " + accountNumber
						+ " with nickNameID : " + nickNameID + " & response : " + channelResponse);
				return data;
			}
			data = channelResponse.getJSONArray("data");
			exchange.setProperty("isSuccessResponse", true);
		} catch (Exception e) {
			log.error("Exception occurred from api : " + apiName + ", for accountNumber : " + accountNumber
					+ " with nickNameID : " + nickNameID + " and response: " + response);
			e.printStackTrace();
		}
		return data;

	}

	private Map<String, List<JSONObject>> separateDataList(JSONArray data, String countryCode)
			throws JSONException {
		Map<String, List<JSONObject>> periodWiseSummaryMap = new HashMap<String, List<JSONObject>>();
		for (int i = data.length() - 1; i >= 0; i--) {
			JSONObject summaryObj = data.getJSONObject(i);
			long settlementStartDate = LazadaUtil.getUnixTimeWithTimeZone(
					summaryObj.getString("created_at").split(" ")[0], "yyyy-MM-dd", countryCode);
			String strSettlementStartDate = LazadaUtil.getPreviousDate(settlementStartDate, DateTimeConstants.TUESDAY,
					countryCode);
			List samePeriodSummaryList = periodWiseSummaryMap.containsKey(strSettlementStartDate)
					? periodWiseSummaryMap.get(strSettlementStartDate)
					: new ArrayList();
			samePeriodSummaryList.add(summaryObj);
			periodWiseSummaryMap.put(strSettlementStartDate, samePeriodSummaryList);
		}
		return periodWiseSummaryMap;
	}

	private Map<String, JSONObject> getPeriodOfWeeksMap(String startPeriod, String endPeriod, String countryCode)
			throws JSONException {
		String dateFormat = "yyyy-MM-dd";
		DateTimeFormatter formatter = DateTimeFormat.forPattern(dateFormat);
		int weekNumber = Weeks
				.weeksBetween(DateTime.parse(startPeriod, formatter), DateTime.parse(endPeriod, formatter)).getWeeks();
		long startUnix = DateUtil.getUnixTimestamp(startPeriod, dateFormat,
				LazadaUtil.timeZoneCountryMap.get(countryCode));
		long endUnix = 0;
		// actualEndUnix -> Jan 31 : 12 am
		long actualEndUnix = DateUtil.getUnixTimestamp(endPeriod, dateFormat,
				LazadaUtil.timeZoneCountryMap.get(countryCode));
		// actualEndUnix -> need as Jan 31 11:59:59 pm
		actualEndUnix += ONE_DAY - 1;
		Map<String, JSONObject> periodOfWeeksMap = new LinkedHashMap<String, JSONObject>();
		for (int i = 0; i <= weekNumber; i++) {
			if (i > 0) {
				startUnix = endUnix + 1;
			}
			endUnix = startUnix + ((7 * ONE_DAY) - 1);
			if (endUnix > actualEndUnix) {
				break;
			}
			JSONObject weekObject = new JSONObject();
			weekObject.put("timeChannelSettlementStartDate", startUnix);
			weekObject.put("timeChannelSettlementEndDate", endUnix);
			weekObject.put("transactionPeriod",
					DateUtil.getDateFromSIAFormat(startUnix, "dd MMM yyyy",
							LazadaUtil.timeZoneCountryMap.get(countryCode)) + " - "
							+ DateUtil.getDateFromSIAFormat(endUnix, "dd MMM yyyy",
									LazadaUtil.timeZoneCountryMap.get(countryCode)));
			periodOfWeeksMap.put(weekObject.getString("transactionPeriod"), weekObject);
		}
		return periodOfWeeksMap;
	}
}
