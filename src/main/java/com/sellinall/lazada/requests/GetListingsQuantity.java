package com.sellinall.lazada.requests;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;

public class GetListingsQuantity implements Processor {
	static Logger log = Logger.getLogger(GetListingsQuantity.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getProperty("request",JSONObject.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String sellerSKU = inBody.getString("sellerSKU");

		String SKU = "";
		if (inBody.has("SKU")) {
			SKU = inBody.getString("SKU");
		}
		JSONObject response = LazadaUtil.getListingQuantities(accountNumber, nickNameID, sellerSKU, SKU);
		List<JSONObject> listings = new ArrayList<JSONObject>();
		if (response != null) {
			JSONArray listing = response.getJSONArray("listing");
			if (listing.length() == 0) {
				log.error("Listing array is empty in quantities API reposne for accountNumber: " + accountNumber
						+ ", nickNameID: " + nickNameID + ", sellerSKU: " + sellerSKU + ", response: " + response);
			}
			for (int i = 0; i < listing.length(); i++) {
				JSONObject listingBody = listing.getJSONObject(i);
				listings.add(listingBody);
			}
		}
		exchange.getOut().setBody(listings);
	}
}