package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.common.FlexiComboDiscountType;

public class InitProcessFlexiCombo implements Processor {
	static Logger log = Logger.getLogger(InitProcessFlexiCombo.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		String accountNumber = inBody.getString("accountNumber");
		try {
			String requestID = inBody.getString("requestID");
			String nickNameID = inBody.getString("nickNameID");
			JSONObject promotionData = inBody.getJSONObject("data");
			String promotionName = promotionData.getString("promotionName");
			String discountType = promotionData.getString("discountType");
			String discountApplyFor = promotionData.getString("discountApplyFor");
			String criteriaType = promotionData.getString("criteriaType");
			int maxOrders = promotionData.getInt("maxOrders");
			long startDate = promotionData.getLong("startDate") * 1000;
			long endDate = promotionData.getLong("endDate") * 1000;
			if (promotionData.has("requestID")) {
				exchange.setProperty("itemRequestID", promotionData.get("requestID"));
			}
			JSONArray criteriaValues = promotionData.getJSONArray("criteriaValues");
			JSONArray discountValues = new JSONArray();
			if (promotionData.has("discountValues")) {
				discountValues = promotionData.getJSONArray("discountValues");
			}
			boolean needToLoadInventory = false;
			if (discountType.equalsIgnoreCase(FlexiComboDiscountType.FREEGIFT.toString())
					|| discountType.equalsIgnoreCase(FlexiComboDiscountType.FREESAMPLE.toString())
					|| discountType.equalsIgnoreCase(FlexiComboDiscountType.FIXEDANDFREEGIFT.toString())
					|| discountType.equalsIgnoreCase(FlexiComboDiscountType.FIXEDANDFREESAMPLE.toString())
					|| discountType.equalsIgnoreCase(FlexiComboDiscountType.PERCENTAGEANDFREEGIFT.toString())
					|| discountType.equalsIgnoreCase(FlexiComboDiscountType.PERCENTAGEANDFREESAMPLE.toString())
					|| discountApplyFor.equalsIgnoreCase("specificproducts")) {
				needToLoadInventory = true;
			}
			List<String> samples = new ArrayList<String>();
			List<String> gifts = new ArrayList<String>();
			List<String> skus = new ArrayList<String>();
			List<String> listOfSKUS = new ArrayList<String>();
			if (needToLoadInventory) {
				if (promotionData.has("sampleSKUs")) {
					JSONArray sampleSKUs = promotionData.getJSONArray("sampleSKUs");
					for (int i = 0; i < sampleSKUs.length(); i++) {
						samples.add(sampleSKUs.getString(i));
						listOfSKUS.add(sampleSKUs.getString(i));
					}
				}
				if (promotionData.has("freeGifts")) {
					JSONArray freeGifts = promotionData.getJSONArray("freeGifts");
					for (int i = 0; i < freeGifts.length(); i++) {
						gifts.add(freeGifts.getString(i));
						listOfSKUS.add(freeGifts.getString(i));
					}
				}
				if (promotionData.has("skus")) {
					JSONArray skusList = promotionData.getJSONArray("skus");
					for (int i = 0; i < skusList.length(); i++) {
						skus.add(skusList.getString(i));
						listOfSKUS.add(skusList.getString(i));
					}
				}
			}
			exchange.setProperty("requestID", requestID);
			exchange.setProperty("accountNumber", accountNumber);
			exchange.setProperty("nickNameID", nickNameID);
			exchange.setProperty("needToLoadInventory", needToLoadInventory);
			exchange.setProperty("samples", samples);
			exchange.setProperty("gifts", gifts);
			exchange.setProperty("skus", skus);
			exchange.setProperty("listOfSKUS", listOfSKUS);

			exchange.setProperty("promotionName", promotionName);
			exchange.setProperty("discountType", discountType);
			exchange.setProperty("discountApplyFor", discountApplyFor);
			exchange.setProperty("criteriaType", criteriaType);
			exchange.setProperty("maxOrders", maxOrders);
			exchange.setProperty("startDate", startDate);
			exchange.setProperty("endDate", endDate);
			exchange.setProperty("criteriaValues", criteriaValues);
			exchange.setProperty("discountValues", discountValues);
		} catch (Exception e) {
			log.error("Exception occurred while processing flexi combo details for accountNumber : "+ accountNumber, e);
		}
	}
}
