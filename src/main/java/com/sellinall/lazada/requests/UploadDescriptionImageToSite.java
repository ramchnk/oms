package com.sellinall.lazada.requests;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.sellinall.util.DateUtil;

public class UploadDescriptionImageToSite implements Processor {
	static Logger log = Logger.getLogger(UploadDescriptionImageToSite.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		BasicDBObject inventory = exchange.getProperty("inventory", BasicDBObject.class);
		BasicDBObject channelData = (BasicDBObject) inventory.get("lazada");
		String SKU = inventory.getString("SKU");
		ArrayList<String> descriptionImageList = exchange.getProperty("descriptionImageList", ArrayList.class);
		Map<String, String> descriptionImageMap = new HashMap<String, String>();
		if (descriptionImageList.size() > 1) {
			String payload = createMultipleImagePayload(descriptionImageList);
			ArrayList<String> newImageURI = new ArrayList<String>();
			UploadImage.callApiToUploadMultipleImages(exchange, payload, SKU, newImageURI);
			if (exchange.getProperties().containsKey("descriptionImageMap")) {
				descriptionImageMap = (Map<String, String>) exchange.getProperty("descriptionImageMap");
			}
		} else {
			String payload = createSingleImagePayload(descriptionImageList);
			String uploadedImageUrl = UploadImage.callApiToUploadSingleImage(exchange, payload, SKU);
			log.info("description image payload is : " + payload + " and response url from API is: " + uploadedImageUrl
					+ " and SKU is : " + SKU);
			descriptionImageMap.put(descriptionImageList.get(0), uploadedImageUrl);
		}
		if (channelData.get("itemDescription") != null) {
			exchange.setProperty("itemDescription",
					changeDescriptionImages(descriptionImageMap, channelData.getString("itemDescription")));
		}
	}

	private String changeDescriptionImages(Map<String, String> descriptionImageMap, String itemDescription) {
		for (Map.Entry<String, String> entry : descriptionImageMap.entrySet()) {
			itemDescription = itemDescription.replaceAll(Pattern.quote(entry.getKey()), entry.getValue());
		}
		return itemDescription;
	}

	private String createSingleImagePayload(ArrayList<String> descriptionImageSet) throws UnsupportedEncodingException {
		String payload = "<?xml version='1.0' encoding='UTF-8' ?>";
		payload += "<Request>";
		payload += "<Image>";
		payload += "<Url>";
		payload += descriptionImageSet.get(0) + "?" + (Long) DateUtil.getSIADateFormat();
		payload += "</Url>";
		payload += "</Image>";
		payload += "</Request>";
		return payload;
	}

	private String createMultipleImagePayload(ArrayList<String> descriptionImageSet)
			throws UnsupportedEncodingException {
		String payload = "<?xml version='1.0' encoding='UTF-8' ?>";
		payload += "<Request>";
		payload += "<Images>";
		for (int i = 0; i < descriptionImageSet.size(); i++) {
			payload += "<Url>";
			payload += descriptionImageSet.get(i) + "?"	+ (Long) DateUtil.getSIADateFormat();
			payload += "</Url>";
		}
		payload += "</Images>";
		payload += "</Request>";
		return payload;
	}

}
