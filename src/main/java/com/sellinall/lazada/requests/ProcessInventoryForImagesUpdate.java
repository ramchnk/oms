package com.sellinall.lazada.requests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.net.URLEncoder;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.DateUtil;

public class ProcessInventoryForImagesUpdate implements Processor {
	static Logger log = Logger.getLogger(ProcessInventoryForImagesUpdate.class.getName());
	private static int RETRY_LIMIT = 1;
	private static int DELAY = 5000;

	public void process(Exchange exchange) throws Exception {
		List<BasicDBObject> inventoryList = exchange.getProperty("inventoryDetails", List.class);
		Map<String, String> failedSKUs = new HashMap<String, String>();
		Map<String, String> sellerSKUMap = new HashMap<String, String>();
		Map<String, List<String>> newImageMap = new HashMap<String, List<String>>();
		for (BasicDBObject inventory : inventoryList) {
			String SKU = inventory.getString("SKU");
			if (SKU.contains("-") || (!SKU.contains("-") && exchange.getProperties().containsKey("isVariantParent")
					&& !exchange.getProperty("isVariantParent", Boolean.class))) {
				constructPayloadForImageMigrate(failedSKUs, newImageMap, exchange, inventory, sellerSKUMap);
			}

		}
		if (newImageMap.size() > 0) {
			createPayloadForImageSet(exchange, newImageMap, failedSKUs);
		}
		if (failedSKUs.size() > 0) {
			exchange.setProperty("imageUpdateFailedsellerSKUMap", failedSKUs);
		}
		exchange.setProperty("sellerSKUMap", sellerSKUMap);
	}

	private void constructPayloadForImageMigrate(Map<String, String> failedSKUs, Map<String, List<String>> newImageMap,
			Exchange exchange, BasicDBObject inventory, Map<String, String> sellerSKUMap) throws JSONException, InterruptedException {
		List<String> newImages = null;
		String SKU = inventory.getString("SKU");
		String sellerSKU = inventory.getString("customSKU");
		sellerSKUMap.put(sellerSKU, SKU);
		if (inventory.containsField("lazada")) {
			BasicDBObject lazada = (BasicDBObject) inventory.get("lazada");
			ArrayList<String> imageURI = (ArrayList<String>) lazada.get("imageURI");
			String imageURL = lazada.getString("imageURL");
			if (imageURI.size() > 1) {
				String payload = constructPayloadForMultipleImages(sellerSKU, imageURL, imageURI);
				newImages = callApiToUploadMultipleImages(failedSKUs, exchange, payload, SKU, sellerSKU);
			} else {
				String payload = constructPayloadForSingleImage(sellerSKU, imageURL, imageURI);
				newImages = callApiToUploadSingleImage(failedSKUs, exchange, payload, SKU, sellerSKU);
			}
			if (newImages.size() > 0) {
				newImageMap.put(sellerSKU, newImages);
			}
		}
	}

	private List<String> callApiToUploadSingleImage(Map<String, String> failedSKUs, Exchange exchange, String payload,
			String SKU, String sellerSKU) {
		ArrayList<String> newImageURI = new ArrayList<String>();
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
			response = NewLazadaConnectionUtil.callAPI(url, "/image/migrate", accessToken, map, requestBody, "", "POST",
					clientID, clientSecret);
			log.debug("Add Item Response : " + response);
			JSONObject channelResponse = new JSONObject(response);
			if (channelResponse.has("data")) {
				JSONObject body = channelResponse.getJSONObject("data");
				if (body.get("image") instanceof JSONObject) {
					newImageURI.add(body.getJSONObject("image").getString("url"));
					return newImageURI;
				}
			} else {
				log.error("image upload failure for SKU : " + SKU + " : with add image Response : " + response);
				failedSKUs.put(sellerSKU,
						"Failure - " + getApiErrorMessageForSingleImage(exchange, channelResponse, SKU));
			}
		} catch (Exception e) {
			log.error("Exception occured while uploading image for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response : " + response);
			e.printStackTrace();
			failedSKUs.put(sellerSKU, "Failure - Image upload failure, please try after some time");
		}
		return newImageURI;
	}

	private String getApiErrorMessageForSingleImage(Exchange exchange, JSONObject response, String sKU) {
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

	private String constructPayloadForSingleImage(String sellerSKU, String imageURL, ArrayList<String> imageURI) {
		String payload = "<?xml version='1.0' encoding='UTF-8' ?>";
		payload += "<Request>";
		payload += "<Image>";
		payload += "<Url>";
		payload += imageURL + imageURI.get(0) + "?" + (Long) DateUtil.getSIADateFormat();
		payload += "</Url>";
		payload += "</Image>";
		payload += "</Request>";
		return payload;
	}

	private String constructPayloadForMultipleImages(String sellerSKU, String imageURL, ArrayList<String> imageURI) {
		String payload = "<?xml version='1.0' encoding='UTF-8' ?>";
		payload += "<Request>";
		payload += "<Images>";
		for (int j = 0; j < imageURI.size(); j++) {
			payload += "<Url>";
			payload += imageURL + imageURI.get(j) + "?" + (Long) DateUtil.getSIADateFormat();
			payload += "</Url>";
		}
		payload += "</Images>";
		payload += "</Request>";
		return payload;
	}

	private List<String> callApiToUploadMultipleImages(Map<String, String> failedSKUs, Exchange exchange,
			String payload, String SKU, String sellerSKU) throws JSONException, InterruptedException {
		ArrayList<String> newImageURI = new ArrayList<String>();
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String url = exchange.getProperty("hostURL", String.class);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("payload", payload);
		String requestBody = "payload=" + URLEncoder.encode(payload);
		String response = NewLazadaConnectionUtil.callAPI(url, "/images/migrate", accessToken, map, requestBody, "",
				"POST", clientID, clientSecret);
		log.info("Images migrate to accountNumber: " + accountNumber + " ,SKU: " + SKU + " and respons is:" + response);
		JSONObject channelResponse = new JSONObject(response);
		if (channelResponse.has("batch_id")) {
			String batchId = channelResponse.getString("batch_id");
			map = new HashMap<String, String>();
			map.put("batch_id", batchId);
			map.put("access_token", accessToken);
			String params = "&batch_id=" + batchId;
			JSONObject batchResponse = uploadAllImages(url, accessToken, map, 0, params, SKU, clientID, clientSecret);
			if (batchResponse.getString("code").equals("0")) {
				JSONObject body = batchResponse.getJSONObject("data");
				if (body.get("images") instanceof JSONObject) {
					exchange.setProperty("isImageUploaded", true);
					newImageURI.add(body.getJSONObject("image").getString("url"));
					return newImageURI;
				} else if (body.get("images") instanceof JSONArray) {
					JSONArray uploadedImages = body.getJSONArray("images");
					exchange.setProperty("isImageUploaded", true);
					for (int i = 0; i < uploadedImages.length(); i++) {
						JSONObject uploadedImageObject = uploadedImages.getJSONObject(i);
						newImageURI.add(uploadedImageObject.getString("url"));
					}
					return newImageURI;
				}
				if (body.has("errors")) {
					log.error("Image upload failure for SKU : " + SKU + " : with add image Response : " + response
							+ " and get image Response : " + batchResponse);
					String failureReason = getApiErrorMessage(body);
					failedSKUs.put(sellerSKU, failureReason);
				}
			} else {
				log.error("Image upload failure for SKU : " + SKU + " : with add image Response : " + response
						+ " and get image Response : " + batchResponse);
				String failureReason = batchResponse.getString("message");
				failedSKUs.put(sellerSKU, failureReason);
			}
		} else {
			log.error("Image upload failure for SKU : " + SKU + " : and Response : " + response);
			failedSKUs.put(sellerSKU, "Failure - image upload failed");
		}
		return newImageURI;
	}

	private String getApiErrorMessage(JSONObject response) {
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

	private JSONObject uploadAllImages(String url, String accessToken, HashMap<String, String> map, int retryCount,
			String params, String SKU, String clientID, String clientSecret)
			throws JSONException, InterruptedException {
		String responseMsg = NewLazadaConnectionUtil.callAPI(url, "/image/response/get", accessToken, map, "", params,
				"GET", clientID, clientSecret);
		log.info("Images response get to SKU: " + SKU + " and respons is:" + responseMsg);
		JSONObject batchResponse = new JSONObject(responseMsg);
		if (batchResponse.has("code") && batchResponse.getString("code").equals("208") && retryCount < RETRY_LIMIT) {
			retryCount++;
			log.warn("Retrying imageUpload For SKU:" + SKU);
			Thread.sleep(DELAY);
			return uploadAllImages(url, accessToken, map, retryCount, params, SKU, clientID, clientSecret);
		}
		return batchResponse;
	}

	private void createPayloadForImageSet(Exchange exchange, Map<String, List<String>> newImageMap,
			Map<String, String> failedSKUs) throws JSONException {
		String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		payload += "<Request>";
		payload += "<Product>";
		payload += "<Skus>";
		for (Entry<String, List<String>> entry : newImageMap.entrySet()) {
			payload += "<Sku>";
			payload += "<SellerSku>" + entry.getKey() + "</SellerSku>";
			payload += "<Images>";
			List<String> imageList = entry.getValue();
			for (int i = 0; i < imageList.size(); i++) {
				payload += "<Image>";
				payload += imageList.get(i);
				payload += "</Image>";
			}
			payload += "</Images>";
			payload += "</Sku>";
		}
		payload += "</Skus>";
		payload += "</Product>";
		payload += "</Request>";
		updateImages(exchange, payload, failedSKUs);
	}

	private void updateImages(Exchange exchange, String payload, Map<String, String> failedSKUs) throws JSONException {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String url = exchange.getProperty("hostURL", String.class);
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("payload", payload);
		String requestBody = "payload=" + URLEncoder.encode(payload);
		String response = NewLazadaConnectionUtil.callAPI(url, "/images/set", accessToken, map, requestBody, "", "POST",
				clientID, clientSecret);
		log.info("images set for accountNumber: " + accountNumber + " and response is: " + response);
		JSONObject channelResponse = new JSONObject(response);
		if (!channelResponse.has("data")) {
			log.error("image upload failure add image update Response : " + response);
		} else {
			JSONObject data = channelResponse.getJSONObject("data");
			if (data.has("warnings")) {
				JSONArray warnings = data.getJSONArray("warnings");
				for (int i = 0; i < warnings.length(); i++) {
					JSONObject warning = warnings.getJSONObject(i);
					if (warning.has("message")) {
						failedSKUs.put(warning.getString("seller_sku"), warning.getString("message"));
					}
				}
			}
		}
	}

}
