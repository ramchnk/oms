/**
 * 
 */
package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.util.JSON;

/**
 * @author vikraman
 *
 */
public class InitializeStartPostRoute implements Processor {
	static Logger log = Logger.getLogger(InitializeStartPostRoute.class.getName());

	public void process(Exchange exchange) throws Exception {

		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		log.debug("InBody: " + inBody);
		exchange.getOut().setBody(inBody);
		String requestType = exchange.getProperty("requestType", String.class);
		// trigger given for active/inActive update case.
		boolean isStatusUpdate = false;
		boolean isImagesUpdate = false;
		boolean isUpdateParentImages = false;
		if (inBody.has("fieldsToUpdate")) {
			JSONArray data = inBody.getJSONArray("fieldsToUpdate");
			ArrayList<String> fieldsToUpdate = new ArrayList<String>();
			for (int i = 0; i < data.length(); i++) {
				fieldsToUpdate.add(data.getString(i));
			}
			if (fieldsToUpdate.contains("status")) {
				isStatusUpdate = true;
			}
			if (fieldsToUpdate.contains("images")) {
				isImagesUpdate = true;
			}
			if (fieldsToUpdate.contains("productImage")) {
				isUpdateParentImages = true;
			}
			exchange.setProperty("fieldsToUpdate", fieldsToUpdate);
		}
		String SKU = null;
		if (requestType.equals("addVariant")) {
			List<String> variantsList = (List<String>) JSON.parse(inBody.getJSONArray("SKU").toString());
			SKU = variantsList.get(0).split("-")[0];
			exchange.setProperty("variantsList", variantsList);
		} else {
			SKU = inBody.getString("SKU");
			// Trigger for individual child SKUs.
			boolean isChildVariantStatusUpdate = false;
			if (SKU.contains("-")) {
				isChildVariantStatusUpdate = true;
			}
			exchange.setProperty("isChildVariantStatusUpdate", isChildVariantStatusUpdate);
		}
		exchange.setProperty("SKU", SKU);
		exchange.setProperty("accountNumber", inBody.getString("accountNumber"));
		if (inBody.has("channel")) {
			exchange.setProperty("channelName", inBody.getString("channel"));
		} else {
			exchange.setProperty("channelName", "lazada");
		}
		if (inBody.has("siteNicknames")) {
			exchange.setProperty("siteNicknames", inBody.get("siteNicknames"));
		}
		if (isImagesUpdate && inBody.has("skuList")) {
			ArrayList<String> skuList = new ArrayList<String>();
			JSONArray skuListArray = inBody.getJSONArray("skuList");
			for (int i = 0; i < skuListArray.length(); i++) {
				if (SKU.equals(skuListArray.getString(i))) {
					isUpdateParentImages = true;
				}
				skuList.add(skuListArray.getString(i));
			}
			exchange.setProperty("skuList", skuList);
		}
		exchange.setProperty("isUpdateParentImages", isUpdateParentImages);
		exchange.setProperty("isStatusUpdate", isStatusUpdate);
		exchange.setProperty("isImagesUpdate", isImagesUpdate);
		if (inBody.has("rowIdentifier")) {
			exchange.setProperty("rowIdentifier", inBody.getJSONObject("rowIdentifier"));
		}
	}
}