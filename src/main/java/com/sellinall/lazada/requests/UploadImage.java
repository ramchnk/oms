package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class UploadImage {
	static Logger log = Logger.getLogger(UploadImage.class.getName());
	private static int RETRY_LIMIT = 3;
	private static int DELAY = 5000;

	public static ArrayList<String> callApiToUploadMultipleImages(Exchange exchange, String payload, String SKU,
			ArrayList<String> newImageURI) {
		Map<String, String> descriptionImageMap = new HashMap<String, String>();
		boolean isDescriptionImages = false;
		List<String> descriptionImageList = new ArrayList<String>();
		if (exchange.getProperties().containsKey("descriptionImageList")) {
			isDescriptionImages = true;
			descriptionImageList = exchange.getProperty("descriptionImageList", ArrayList.class);
		}
		//exchange.setProperty("isImageUploaded", false);
		String accessToken = exchange.getProperty("accessToken", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		String url = exchange.getProperty("hostURL", String.class);
		map.put("access_token", accessToken);
		map.put("payload", payload);
		String requestBody = "payload=" + URLEncoder.encode(payload);
		String response = "";
		try {
			String clientID = Config.getConfig().getLazadaClientID();
			String clientSecret = Config.getConfig().getLazadaClientSecret();
			response = NewLazadaConnectionUtil.callAPI(url, "/images/migrate", accessToken, map, requestBody, "",
					"POST", clientID, clientSecret);
			log.debug("Add Item Response : " + response);
			JSONObject channelResponse = new JSONObject(response);
			if (channelResponse.has("batch_id")) {
				String batchId = channelResponse.getString("batch_id");
				if (exchange.getProperties().containsKey("accountNumber")
						&& exchange.getProperty("accountNumber", String.class) != null && Config.getConfig()
								.getTestAccountNumber().equals(exchange.getProperty("accountNumber", String.class))) {
					log.info("batchId for accountNumber : " + exchange.getProperty("accountNumber", String.class)
							+ " and SKU : " + SKU + " and batchId is : " + batchId.toString());
				}
				map = new HashMap<String, String>();
				map.put("batch_id", batchId);
				map.put("access_token", accessToken);
				String params = "&batch_id=" + batchId;
				JSONObject batchResponse = uploadAllImages(response, url, accessToken, map, 0, params, SKU, clientID, clientSecret);
				if (batchResponse.getString("code").equals("0")) {
					List<String> errorImageList = new ArrayList<String>();
					List<String> uploadedImageList = new ArrayList<String>();
					JSONObject body = batchResponse.getJSONObject("data");
					if (body.get("images") instanceof JSONObject) {
						if (isDescriptionImages) {
							uploadedImageList.add(body.getJSONObject("image").getString("url"));
						} else {
							//exchange.setProperty("isImageUploaded", true);
							newImageURI.add(body.getJSONObject("image").getString("url"));
						}
					} else if (body.get("images") instanceof JSONArray) {
						if (isDescriptionImages) {
							if (body.has("errors")) {
								JSONArray errors = body.getJSONArray("errors");
								for (int i = 0; i < errors.length(); i++) {
									JSONObject error = errors.getJSONObject(i);
									String img = error.getString("msg");
									errorImageList.add(img.split("[?]")[0]);
								}
							}
							JSONArray uploadedImages = body.getJSONArray("images");
							for (int i = 0; i < uploadedImages.length(); i++) {
								JSONObject uploadedImageObject = uploadedImages.getJSONObject(i);
								uploadedImageList.add(uploadedImageObject.getString("url"));
							}
						} else {
							JSONArray uploadedImages = body.getJSONArray("images");
							//exchange.setProperty("isImageUploaded", true);
							for (int i = 0; i < uploadedImages.length(); i++) {
								JSONObject uploadedImageObject = uploadedImages.getJSONObject(i);
								newImageURI.add(uploadedImageObject.getString("url"));
							}
						}
					}
					int j = 0;
					for (int i = 0; i < descriptionImageList.size(); i++) {
						if (!errorImageList.contains(descriptionImageList.get(i))) {
							if (uploadedImageList.size() > j) {
								descriptionImageMap.put(descriptionImageList.get(i), uploadedImageList.get(j));
								j++;
							}
						}
					}
					exchange.setProperty("descriptionImageMap", descriptionImageMap);
					if (body.has("errors")) {
						//exchange.setProperty("isImageUploaded", false);
						log.error("image upload failure for SKU : " + SKU + " : with add image Response : " + response
								+ " and get image Response : " + batchResponse);
						//exchange.setProperty("isPostingSuccess", false);
						String failureReason = getApiErrorMessage(body);
						exchange.setProperty("warningMessage", "Warning - " + failureReason);
					}
					return newImageURI;
				} else {
					//exchange.setProperty("isImageUploaded", false);
					log.error("image upload failure for SKU : " + SKU + " : with add image Response : " + response
							+ " and get image Response : " + batchResponse);
					//exchange.setProperty("isPostingSuccess", false);
					String failureReason = batchResponse.getString("message");
					exchange.setProperty("warningMessage", "Warning - " + failureReason);
				}
			} else {
				//exchange.setProperty("isImageUploaded", false);
				log.error("image upload failure SKU : " + SKU + " : and Response : " + response);
				//exchange.setProperty("isPostingSuccess", false);
				exchange.setProperty("warningMessage", "Warning - image upload failed");
			}
		} catch (Exception e) {
			log.error("Exception occured while uploading image for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response : " + response);
			e.printStackTrace();
			//exchange.setProperty("isPostingSuccess", false);
			exchange.setProperty("warningMessage", "Warning - Internal error");
		}
		return new ArrayList<String>();
	}

	private static String getApiErrorMessage(JSONObject response) {
		try {
			String errorMessage = "";
			JSONArray details = new JSONArray();
			if (response.get("errors") instanceof JSONObject) {
				details.put(response.getJSONObject("errors"));
			} else if (response.get("errors") instanceof JSONArray) {
				details = response.getJSONArray("errors");
			}
			for (int i = 0; i < details.length(); i++) {
				JSONObject detail = details.getJSONObject(i);
				if (detail.has("field")) {
					errorMessage += (errorMessage.isEmpty() ? "" : ",") + detail.getString("field") + ":"
							+ detail.getString("msg");
				} else {
					errorMessage += (errorMessage.isEmpty() ? "" : ",") + detail.getString("msg");
				}
			}
			return errorMessage;
		} catch (Exception e) {
			log.error("Image upload failure");
		}
		return "Image upload failure, please try after some time";
	}

	private static JSONObject uploadAllImages(String response, String url, String accessToken, HashMap<String, String> map,
			int retryCount, String params, String SKU, String clientID, String clientSecret) throws Exception {
		String responseMsg = NewLazadaConnectionUtil.callAPI(url, "/image/response/get", accessToken, map, "", params,
				"GET", clientID, clientSecret);
		JSONObject batchResponse = new JSONObject(responseMsg);
		if (batchResponse.has("code") && batchResponse.getString("code").equals("208") && retryCount < RETRY_LIMIT) {
			retryCount++;
			log.info("Retrying imageUpload For SKU:" + SKU);
			Thread.sleep(DELAY);
			return uploadAllImages(response, url, accessToken, map, retryCount, params, SKU, clientID, clientSecret);
		}
		return batchResponse;
	}

	public static String callApiToUploadSingleImage(Exchange exchange, String payload, String SKU) {
		//exchange.setProperty("isImageUploaded", false);
		String accessToken = exchange.getProperty("accessToken", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		String url = exchange.getProperty("hostURL", String.class);
		map.put("access_token", accessToken);
		map.put("payload", payload);
		String requestBody = "payload=" + URLEncoder.encode(payload);
		String response = "";
		try {
			String clientID = Config.getConfig().getLazadaClientID();
			String clientSecret = Config.getConfig().getLazadaClientSecret();
			response = NewLazadaConnectionUtil.callAPI(url, "/image/migrate", accessToken, map, requestBody, "",
					"POST", clientID, clientSecret);
			log.debug("Add Item Response : " + response);
			JSONObject channelResponse = new JSONObject(response);
			if (channelResponse.has("data")) {
				JSONObject body = channelResponse.getJSONObject("data");
				if (body.get("image") instanceof JSONObject) {
					//exchange.setProperty("isImageUploaded", true);
					return body.getJSONObject("image").getString("url");
				}
			} else {
				log.error("image upload failure for SKU : " + SKU + " : with add image Response : " + response);
				//exchange.setProperty("isPostingSuccess", false);
				exchange.setProperty("warningMessage",
						"Warning - " + getApiErrorMessageForSingleImage(exchange, channelResponse));
			}
		} catch (Exception e) {
			log.error("Exception occured while uploading image for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response : " + response);
			e.printStackTrace();
			//exchange.setProperty("isPostingSuccess", false);
			exchange.setProperty("warningMessage", "Warning - Internal error");
		}
		return "";
	}

	private static String getApiErrorMessageForSingleImage(Exchange exchange, JSONObject response) {

		try {
			String errorMessage = "";
			if (response.has("detail")) {
				JSONArray details = response.getJSONArray("detail");
				for (int i = 1; i < details.length(); i++) {
					JSONObject detail = details.getJSONObject(i);
					if (detail.has("field")) {
						errorMessage += (errorMessage.isEmpty() ? "" : ",") + detail.getString("field") + ":"
								+ detail.getString("message");
					} else {
						errorMessage += (errorMessage.isEmpty() ? "" : ",") + detail.getString("message");
					}
				}
			}
			return response.getString("message") + (errorMessage.isEmpty() ? "" : "-") + errorMessage;
		} catch (Exception e) {
			log.error("Image upload failure");
		}
		return "Image upload failure, please try after some time";
	}

}
