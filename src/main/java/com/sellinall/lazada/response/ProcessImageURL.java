package com.sellinall.lazada.response;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;

public class ProcessImageURL implements Processor {
	static Logger log = Logger.getLogger(ProcessImageURL.class.getName());

	public void process(Exchange exchange) throws Exception {
		String uploadImageToSellInAllURL = Config.getConfig().getUploadImageToSellInAllUrl();
		String merchantID = exchange.getProperty("merchantID", String.class);
		String SKU = exchange.getProperty("SKU", String.class);
		String parentSKU= SKU.split("-")[0];
		if(SKU.contains("U-")){
			parentSKU =  parentSKU + "-" + SKU.split("-")[1];
		}
		SKU = parentSKU;
		boolean isNewRecord = exchange.getProperty("isNewItem", Boolean.class);
		boolean isResetExistingImage = true;

		JSONArray productImage = exchange.getProperty("itemImages", JSONArray.class);
		if (productImage != null) {
			isResetExistingImage = false;
			HashMap<String, Object> itemImages = new HashMap<String, Object>();
			Map<String, String> siaItemImages = uploadImageToSIA(merchantID, parentSKU, uploadImageToSellInAllURL,
					getImages(productImage), isNewRecord, true);
			itemImages.put(parentSKU, siaItemImages);
			exchange.setProperty("siaItemImages", itemImages);
		}

		HashMap<String, Object> images = new HashMap<String, Object>();
		JSONArray SKUS = exchange.getProperty("SKUS", JSONArray.class);
		for (int i = 0; i < SKUS.length(); i++) {
			Map<String, String> siaImages = new LinkedHashMap<String, String>();
			JSONArray imageFromSite = new JSONArray();
			JSONObject SKUObject = SKUS.getJSONObject(i);
			NumberFormat numberFormat = new DecimalFormat("00");
			String variantCounterString = numberFormat.format(i + 1).toString();
			String variantSKU = SKU;
			String skuId = SKUObject.getString("SkuId");
			if (exchange.getProperty("itemHasVariants", Boolean.class)) {
				// Child only have -00 values
				variantSKU = SKU + "-" + variantCounterString;
				isResetExistingImage = true;
			}
			if(SKUObject.has("MainImage")) {
				imageFromSite.put(SKUObject.getString("MainImage"));
				for(int index=2;index<=8;index++) {
					if(SKUObject.has("Image"+index)) {
						imageFromSite.put(SKUObject.getString("Image"+index));
					}
				}
			} else if(SKUObject.has("Images")){
				imageFromSite = SKUObject.getJSONArray("Images");
			}
			if(imageFromSite.length() > 0) {
				siaImages = uploadImageToSIA(merchantID, variantSKU, uploadImageToSellInAllURL,
						getImages(imageFromSite), isNewRecord, isResetExistingImage);
			}
			if (siaImages != null && siaImages.keySet().size() > 0) {
				images.put(skuId, siaImages);
			} else if (productImage != null && !exchange.getProperty("itemHasVariants", Boolean.class)) {
				images.put(skuId, siaImages);
			} else {
				//Simply upload no image to SIA server
				siaImages = uploadImageToSIA(merchantID, variantSKU, uploadImageToSellInAllURL,
						new LinkedHashSet<String>(), isNewRecord, isResetExistingImage);
				images.put(skuId, siaImages);
			}
		}
		exchange.setProperty("siaImages", images);
	}

	private static Set<String> getImages(JSONArray images) throws JSONException {
		Set<String> imageSet = new LinkedHashSet<String>();
		for (int i = 0; i < images.length(); i++) {
			String image = images.getString(i);
			// Some time we got some empty string array
			if (!image.equals("")) {
				log.debug("response images " + image);
				if (!image.contains("http")) {
					image = "http:" + image;
				}
				if(image.contains("-catalog.jpg")){
					image = image.replace("-catalog.jpg", "-zoom.jpg");
				}
				imageSet.add(image);
			}
		}
		return imageSet;
	}

	private static Map<String, String> uploadImageToSIA(String merchantID, String SKU, String url, Set<String> imageSet,
			Boolean isNewRecord, boolean isResetExistingImage) throws JSONException, IOException {
		Map<String, String> map = new LinkedHashMap<String, String>();
		JSONObject imagesPayload = new JSONObject();
		imagesPayload.put("merchantID", merchantID);
		imagesPayload.put("uniqueUploadID", SKU);
		if (imageSet.isEmpty() && isNewRecord) {
			imageSet.add(Config.getConfig().getNoImageURL());
		}
		if (!imageSet.isEmpty()) {
			// If it is already exist then we will delete existing image and re
			// upload new images
			if (isResetExistingImage) {
				imagesPayload.put("reset", 1);
			} else {
				imagesPayload.put("reset", 0);
			}
			imagesPayload.put("url", imageSet);
			log.debug("imagesPayload:" + imagesPayload);
			map = callSIAServerToUploadImage(imagesPayload, url, merchantID, SKU, imageSet);
		}
		return map;
	}

	private static Map<String, String> callSIAServerToUploadImage(JSONObject imagesPayload, String url,
			String merchantID, String SKU, Set<String> imageSet) throws JSONException, IOException {
		Map<String, String> config = new HashMap<String, String>();
		config.put("Content-Type", "application/json");
		config.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		JSONObject response = HttpsURLConnectionUtil.doPut(url, imagesPayload.toString(), config);
		if (response.getInt("httpCode") != 200 || !response.has("payload")) {
			log.error("Upload image reponse empty for SKU : " + SKU + " and merchantID : " + merchantID
					+ " and the images are : " + imagesPayload.toString());
			return null;
		}
		String imageUploadResponse = response.getString("payload");
		JSONObject uploadResponse = new JSONObject(imageUploadResponse);
		log.debug("uploadResponse:" + uploadResponse);
		JSONArray imageArray = uploadResponse.getJSONArray("response");
		Map<String, String> imageMap = new LinkedHashMap<String, String>();
		for (int i = 0; i < imageArray.length(); i++) {
			JSONObject imageItem = imageArray.getJSONObject(i);
			imageMap.put(imageItem.getString("input"), imageItem.getString("output"));
		}
		if (imageMap.size() < imageSet.size()) {
			log.info("Image upload request and response count mismatch for SKU: " + SKU + ",  and merchantID :"
					+ merchantID + " request " + imageSet.toString() + " and response is" + imageArray.toString());
		}
		log.debug("imageMap:" + imageMap);
		return imageMap;
	}
	
}