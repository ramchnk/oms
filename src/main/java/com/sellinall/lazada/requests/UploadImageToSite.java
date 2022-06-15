package com.sellinall.lazada.requests;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.DateUtil;

public class UploadImageToSite implements Processor {
	static Logger log = Logger.getLogger(UploadImageToSite.class.getName());
	private static int RETRY_LIMIT = 3;
	private static int DELAY = 5000;

	public void process(Exchange exchange) throws Exception {
		ArrayList<BasicDBObject> inventoryList = exchange.getProperty("inventoryDetails", ArrayList.class);
		exchange.setProperty("isImageUploaded", true);
		for (BasicDBObject inventory : inventoryList) {
			String SKU = "";
			if (inventory.containsField("SKU")) {
				SKU = inventory.getString("SKU");
			}
			BasicDBObject channel = (BasicDBObject) inventory.get(exchange.getProperty("channelName", String.class));
			ArrayList<String> newImageURI = new ArrayList<String>();
			ArrayList<String> newProductImageURI = new ArrayList<String>();

			if (channel.containsField("imageURI")) {
				ArrayList<String> imageURI = (ArrayList<String>) channel.get("imageURI");
				if (exchange.getProperties().containsKey("accountNumber")
						&& exchange.getProperty("accountNumber", String.class) != null && Config.getConfig()
								.getTestAccountNumber().equals(exchange.getProperty("accountNumber", String.class))) {
					log.info("imageURI for accountNumber : " + exchange.getProperty("accountNumber", String.class)
								+ " and SKU : " + SKU + " and newImageURI is : " + imageURI);
				}
				if (imageURI.size() > 1) {
					String payload = createMultipleImagePayload(imageURI, channel, inventory);
					if (exchange.getProperties().containsKey("accountNumber")
							&& exchange.getProperty("accountNumber", String.class) != null
							&& Config.getConfig().getTestAccountNumber()
									.equals(exchange.getProperty("accountNumber", String.class))) {
						log.info("payload for accountNumber : " + exchange.getProperty("accountNumber", String.class)
								+ " and SKU : " + SKU + " and payload is : " + payload.toString());
					}
					log.debug("Add item payLoad =" + payload);
					newImageURI = callApiToUploadMultipleImages(exchange, payload, inventory.getString("SKU"),
							newImageURI);
					if (exchange.getProperties().containsKey("accountNumber")
							&& exchange.getProperty("accountNumber", String.class) != null
							&& Config.getConfig().getTestAccountNumber()
									.equals(exchange.getProperty("accountNumber", String.class))) {
						log.info("newImageURI for accountNumber : "
								+ exchange.getProperty("accountNumber", String.class) + " and SKU : " + SKU
								+ " and newImageURI is : " + newImageURI.toString());
					}
					if (exchange.getProperties().containsKey("isPostingSuccess")
							&& !exchange.getProperty("isPostingSuccess", boolean.class)) {
						// If any image uploaded failed then will stop the flow
						return;
					}
				} else if (imageURI.size() == 1) {
					String payload = createSingleImagePayload(imageURI.get(0), channel, inventory);
					String uploadedImageUrl = callApiToUploadSingleImage(exchange, payload, inventory.getString("SKU"));
					if(uploadedImageUrl.isEmpty()){
						//If any image uploaded failed then will stop the flow
						return;
					}
					newImageURI.add(uploadedImageUrl);
				}
			}
			if (channel.containsField("productImageURI")) {
				ArrayList<String> productImageURI = (ArrayList<String>) channel.get("productImageURI");
				if (exchange.getProperties().containsKey("accountNumber")
						&& exchange.getProperty("accountNumber", String.class) != null && Config.getConfig()
								.getTestAccountNumber().equals(exchange.getProperty("accountNumber", String.class))) {
					log.info("productImageURI for accountNumber : " + exchange.getProperty("accountNumber", String.class)
								+ " and SKU : " + SKU + " and productImageURI is : " + productImageURI);
				}
				if (productImageURI.size() > 1) {
					String payload = createMultipleImagePayload(productImageURI, channel, inventory);
					if (exchange.getProperties().containsKey("accountNumber")
							&& exchange.getProperty("accountNumber", String.class) != null
							&& Config.getConfig().getTestAccountNumber()
									.equals(exchange.getProperty("accountNumber", String.class))) {
						log.info("payload for accountNumber : " + exchange.getProperty("accountNumber", String.class)
								+ " and SKU : " + SKU + " and payload is : " + payload.toString());
					}
					log.debug("Add item payLoad =" + payload);
					newProductImageURI = callApiToUploadMultipleImages(exchange, payload, inventory.getString("SKU"),
							newProductImageURI);
					if (exchange.getProperties().containsKey("accountNumber")
							&& exchange.getProperty("accountNumber", String.class) != null
							&& Config.getConfig().getTestAccountNumber()
									.equals(exchange.getProperty("accountNumber", String.class))) {
						log.info("productImageURI for accountNumber : "
								+ exchange.getProperty("accountNumber", String.class) + " and SKU : " + SKU
								+ " and newProductImageURI is : " + newProductImageURI.toString());
					}
					if (exchange.getProperties().containsKey("isPostingSuccess")
							&& !exchange.getProperty("isPostingSuccess", boolean.class)) {
						// If any image uploaded failed then will stop the flow
						return;
					}
				} else if (productImageURI.size() == 1) {
					String payload = createSingleImagePayload(productImageURI.get(0), channel, inventory);
					String uploadedImageUrl = callApiToUploadSingleImage(exchange, payload, inventory.getString("SKU"));
					if(uploadedImageUrl.isEmpty()){
						//If any image uploaded failed then will stop the flow
						return;
					}
					newProductImageURI.add(uploadedImageUrl);
				}
			}
			if (newProductImageURI.size() > 0) {
				if(!SKU.contains("-")) {
					exchange.setProperty("newProductImageURI", newProductImageURI);
				}
				channel.put("newProductImageURI", newProductImageURI);
			}
			if (newImageURI.size() > 0) {
				if(!SKU.contains("-")) {
					exchange.setProperty("newImageURI", newImageURI);
				}
				channel.put("newImageURI", newImageURI);
			}
		}
	}

	private String callApiToUploadSingleImage(Exchange exchange, String payload, String SKU) throws IOException {
		exchange.setProperty("isImageUploaded", false);
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
					exchange.setProperty("isImageUploaded", true);
					return body.getJSONObject("image").getString("url");
				}
			} else {
				log.error("image upload failure for SKU : " + SKU + " : with add image Response : " + response);
				exchange.setProperty("isPostingSuccess", false);
				exchange.setProperty("failureReason",
						"Failure - " + getApiErrorMessageForSingleImage(exchange, channelResponse, SKU));
			}
		} catch (Exception e) {
			log.error("Exception occured while uploading image for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response : " + response);
			e.printStackTrace();
			exchange.setProperty("isPostingSuccess", false);
			exchange.setProperty("failureReason", "Failure - Image uploading failed & error - " + e.getMessage());
		}
		return "";
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

	private ArrayList<String> callApiToUploadMultipleImages(Exchange exchange, String payload, String SKU, ArrayList<String> newImageURI)
			throws IOException {
		exchange.setProperty("isImageUploaded", false);
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
						exchange.setProperty("isImageUploaded", false);
						log.error("image upload failure for SKU : " + SKU + " : with add image Response : " + response
								+ " and get image Response : " + batchResponse);
						exchange.setProperty("isPostingSuccess", false);
						String failureReason = getApiErrorMessage(body);
						exchange.setProperty("failureReason", "Failure - " + failureReason);
					}
				} else {
					exchange.setProperty("isImageUploaded", false);
					log.error("image upload failure for SKU : " + SKU + " : with add image Response : " + response
							+ " and get image Response : " + batchResponse);
					exchange.setProperty("isPostingSuccess", false);
					String failureReason = batchResponse.getString("message");
					exchange.setProperty("failureReason", "Failure - " + failureReason);
				}
			} else {
				exchange.setProperty("isImageUploaded", false);
				log.error("image upload failure SKU : " + SKU + " : and Response : " + response);
				exchange.setProperty("isPostingSuccess", false);
				exchange.setProperty("failureReason", "Failure - Image upload failed, please try again");
			}
		} catch (Exception e) {
			log.error("Exception occured while uploading image for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response : " + response);
			e.printStackTrace();
			exchange.setProperty("isPostingSuccess", false);
			exchange.setProperty("failureReason", "Failure - Image uploading failed & error - " + e.getMessage());
		}
		return new ArrayList<String>();
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

	private String createMultipleImagePayload(ArrayList<String> imageURI, BasicDBObject channel, BasicDBObject inventory) {
		String payload = "<?xml version='1.0' encoding='UTF-8' ?>";
		payload += "<Request>";
		payload += "<Images>";
		String imageURL = "";
		if (channel.containsField("imageURL")) {
			imageURL = channel.getString("imageURL");
		} else {
			imageURL = inventory.getString("imageURL");
		}
		for (String image : imageURI) {
			payload += "<Url>";
			payload += imageURL + image + "?" + (Long) DateUtil.getSIADateFormat();
			payload += "</Url>";
		}
		payload += "</Images>";
		payload += "</Request>";
		return payload;
	}

	private String createSingleImagePayload(String imageURI, BasicDBObject channel, BasicDBObject inventory){
		String payload = "<?xml version='1.0' encoding='UTF-8' ?>";
		payload += "<Request>";
		payload += "<Image>";
		payload += "<Url>";
		String imageURL = "";
		if(channel.containsField("imageURL")){
			imageURL = channel.getString("imageURL");
		}else{
			imageURL = inventory.getString("imageURL");
		}
		payload += imageURL + imageURI + "?" + (Long) DateUtil.getSIADateFormat();
		payload += "</Url>";
		payload += "</Image>";
		payload += "</Request>";
		return payload;
	}

	private JSONObject uploadAllImages(String response, String url, String accessToken, HashMap<String, String> map,
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
}
