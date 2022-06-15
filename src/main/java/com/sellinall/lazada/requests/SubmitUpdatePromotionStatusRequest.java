package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.enums.PromotionType;

public class SubmitUpdatePromotionStatusRequest implements Processor {

	static Logger log = Logger.getLogger(SubmitUpdatePromotionStatusRequest.class.getName());
	private static final int MAX_RETRY_COUNT = 3;

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		String promotionID = exchange.getProperty("promotionID", String.class);
		String promotionType = exchange.getProperty("promotionType", String.class);
		String status = exchange.getProperty("status", String.class);
		String voucherType = "";
		if (exchange.getProperties().containsKey("voucherType")) {
			voucherType = exchange.getProperty("voucherType", String.class);
		}

		String endpoint = getApiEndPoint(promotionType, status);

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("id", promotionID);

		String queryParams = "&id=" + promotionID;

		if (!voucherType.isEmpty()) {
			queryParams += "&voucher_type=" + voucherType;
			map.put("voucher_type", voucherType);
		}

		JSONObject response = callAPI(accountNumber, nickNameID, hostURL, endpoint, accessToken, map, queryParams, 1);
		if (response.has("failureReason")) {
			exchange.setProperty("failureReason", response.getString("failureReason"));
		}
	}

	private String getApiEndPoint(String promotionType, String status) {
		if (status.equals("ACTIVE")) {
			if (promotionType.equals(PromotionType.VOUCHER.toString())) {
				return "/promotion/voucher/activate";
			} else if (promotionType.equals(PromotionType.FREE_SHIPPING.toString())) {
				return "/promotion/freeshipping/activate";
			}
		} else {
			if (promotionType.equals(PromotionType.VOUCHER.toString())) {
				return "/promotion/voucher/deactivate";
			} else if (promotionType.equals(PromotionType.FREE_SHIPPING.toString())) {
				return "/promotion/freeshipping/deactivate";
			}
		}
		return null;
	}

	private JSONObject callAPI(String accountNumber, String nickNameID, String hostURL, String endpoint,
			String accessToken, HashMap<String, String> map, String queryParams, int retryCount) {
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();

		JSONObject responseObj = new JSONObject();
		try {
			String response = NewLazadaConnectionUtil.callAPI(hostURL, endpoint, accessToken, map, "", queryParams,
					"POST", clientID, clientSecret);

			if (response.isEmpty() || !response.startsWith("{")) {
				log.error("Getting invalid response for accountNumber : " + accountNumber + ", nickNameID : "
						+ nickNameID + ", promotionID : " + map.get("id") + " & response : " + response);
				if (retryCount <= MAX_RETRY_COUNT) {
					retryCount++;
					return callAPI(accountNumber, nickNameID, hostURL, endpoint, accessToken, map, queryParams,
							retryCount);
				} else {
					responseObj.put("status", "failure");
					responseObj.put("failureReason", "Getting invalid response from Lazada, please try again");
					return responseObj;
				}
			}
			JSONObject responseObject = new JSONObject(response);
			if (!responseObject.getString("code").equals("0")) {
				log.error("Getting failure response for accountNumber : " + accountNumber + ", nickNameID : "
						+ nickNameID + ", promotionID : " + map.get("id") + " & response : " + response);
				if (retryCount <= MAX_RETRY_COUNT) {
					retryCount++;
					return callAPI(accountNumber, nickNameID, hostURL, endpoint, accessToken, map, queryParams,
							retryCount);
				} else {
					String msg = responseObject.has("message") ? responseObject.getString("message")
							: "Getting invalid response from Lazada, please try again";
					responseObj.put("status", "failure");
					responseObj.put("failureReason", msg);
					return responseObj;
				}
			}
			String successValue = responseObject.getString("success");
			if (successValue.equals("true")) {
				responseObj.put("status", "success");
			} else {
				responseObj.put("status", "failure");
				responseObj.put("failureReason", responseObject.getString("error_msg"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return responseObj;
	}

}
