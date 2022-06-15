package com.sellinall.lazada.validation;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;

public class ValidateImage implements Processor {
	static Logger log = Logger.getLogger(ValidateImage.class.getName());

	public void process(Exchange exchange) throws Exception {
		String channelName = exchange.getProperty("channelName", String.class);
		String imageValidateUrl = Config.getConfig().getValidateImageServer();
		JSONObject imagesPayload = new JSONObject();
		imagesPayload.put("channelName", channelName);
		imagesPayload.put("url",
				getImageURI(exchange.getProperty("inventoryDetails", ArrayList.class),channelName));
		Map<String, String> config = new HashMap<String, String>();
		config.put("Content-Type", "application/json");
		config.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		String failureReason = "";
		if(exchange.getProperties().containsKey("failureReason")) {
			failureReason = exchange.getProperty("failureReason", String.class);
		}
		try {
			JSONObject imageUploadResponseJSON = HttpsURLConnectionUtil.doPost(imageValidateUrl, imagesPayload.toString(),
					config);
			String imageUploadResponse = imageUploadResponseJSON.getString("payload");
			if (imageUploadResponse != null && imageUploadResponse.trim().startsWith("{")) {
				JSONObject validationResponse = new JSONObject(imageUploadResponse);
				if (validationResponse.getBoolean("validation")) {
					exchange.setProperty("isValidateImageSize", true);
					return;
				} else if (validationResponse.has("errorMessage")) {
					if (!failureReason.isEmpty()) {
						failureReason += "\n" + validationResponse.getString("errorMessage");
					} else {
						failureReason = validationResponse.getString("errorMessage");
					}
				}
			} else {
				failureReason += (failureReason.isEmpty() ? "" : "\n") + "Image upload failed, please try again later";
				log.error("Image validation fail,  Request: " + imagesPayload.toString() + " and reponse "
						+ imageUploadResponseJSON);
			}

			exchange.setProperty("isValidateImageSize", false);
			exchange.setProperty("isPostingSuccess", false);
			exchange.setProperty("failureReason", failureReason);
		}catch (Exception e) {
			failureReason += (failureReason.isEmpty() ? "" : "\n") + "Image upload failed, please try again later";
			exchange.setProperty("isPostingSuccess", false);
			exchange.setProperty("failureReason", failureReason);
			e.printStackTrace();
		}
	}

	private ArrayList<String> getImageURI(ArrayList<BasicDBObject> inventoryList,
			String channelName) throws UnsupportedEncodingException {
		ArrayList<String> imageArray = new ArrayList<String>();
		for (BasicDBObject inventory : inventoryList) {
			BasicDBObject channel = (BasicDBObject) inventory.get(channelName);
			String imageUrl = "";
			if (channel.containsField("imageURL")) {
				imageUrl = channel.getString("imageURL");
			} else {
				imageUrl = inventory.getString("imageURL");
			}
			ArrayList<String> imageUri = new ArrayList<String>();
			if (channel.containsField("imageURI")) {
				imageUri = (ArrayList<String>) channel.get("imageURI");
			}
			for (int i = 0; i < imageUri.size(); i++) {
				String[] imageURI = imageUri.get(i).split("/");
				String encodedURI = "";
				for (String uri : imageURI) {
					//image is already encoded in validation, will decode and encode to avoid errors.
					encodedURI += encodedURI.isEmpty() ? URLEncoder.encode(uri, "UTF-8")
							: "/" + URLEncoder.encode(URLDecoder.decode(uri, "UTF-8"), "UTF-8");
				}
				String image = imageUrl + encodedURI;
				imageArray.add(image);
			}
		}
		return imageArray;
	}
}
