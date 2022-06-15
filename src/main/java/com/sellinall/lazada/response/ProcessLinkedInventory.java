package com.sellinall.lazada.response;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.DateUtil;
import com.sellinall.util.enums.SIAInventoryStatus;

public class ProcessLinkedInventory implements Processor {
	static Logger log = Logger.getLogger(ProcessLinkedInventory.class.getName());

	public void process(Exchange exchange) throws Exception {
		ArrayList<BasicDBObject> updateList = new ArrayList<BasicDBObject>();
		JSONObject item = exchange.getProperty("item", JSONObject.class);
		JSONObject attributes = item.getJSONObject("Attributes");
		JSONArray SKUs = exchange.getProperty("SKUS", JSONArray.class);
		/*
		 * HashMap<String, Map<String, String>> SIAImages =
		 * exchange.getProperty("SIAImages", HashMap.class);
		 */
		for (int i = 0; i < SKUs.length(); i++) {
			JSONObject SKUDetails = SKUs.getJSONObject(i);
			BasicDBObject channel = new BasicDBObject();
			/*
			 * BasicDBObject inventoryChannel =
			 * exchange.getProperty("inventoryChannel", BasicDBObject.class);
			 */
			String status = SKUDetails.getString("Status");
			String channelName = exchange.getProperty("channelName", String.class);
			if (status.equals("active")) {
				channel.put(channelName + ".$.status", SIAInventoryStatus.ACTIVE.toString());
			} else {
				// Non Active Item (Like a Out of stack)
				channel.put(channelName + ".$.status", SIAInventoryStatus.INACTIVE.toString());
			}
			exchange.setProperty("publishMessageToFB", true);
			if (SKUDetails.has("Url") && !SKUDetails.getString("Url").equals("")) {
				// Item Waiting for Approval
				channel.put(channelName + ".$.itemUrl", SKUDetails.getString("Url"));
			} else {
				channel.put(channelName + ".$.status", SIAInventoryStatus.PENDING.toString());
				// if it is polling then will publish message to FB if required
				/*
				 * if (inventoryChannel.containsField("itemUrl")) { // No need
				 * to publish message exchange.setProperty("publishMessageToFB",
				 * false); }
				 */
			}
			channel.put(channelName + ".$.categoryID", item.get("PrimaryCategory") + "");
			channel.put("categoryName", LazadaUtil.getCategoryName(
					exchange.getProperty("countryCode", String.class), item.getString("PrimaryCategory")));
			channel.put(channelName + ".$.noOfItem", SKUDetails.getInt("quantity"));
			if (SKUDetails.has("Available")) {
				channel.put(channelName + ".$.available", SKUDetails.getInt("Available"));
			}
			if (SKUDetails.has("price")) {
				double itemAmountStr = Double.parseDouble(SKUDetails.getString("price"));
				long itemAmount = (long) (itemAmountStr * 100);
				channel.put(channelName + ".$.itemAmount",
						CurrencyUtil.getAmountObject(itemAmount, exchange.getProperty("currencyCode", String.class)));
			} else {
				channel.put(channelName + ".$.itemAmount",
						CurrencyUtil.getAmountObject(0, exchange.getProperty("currencyCode", String.class)));
			}
			if (attributes.has("brand")) {
				channel.put(channelName + ".$.brand", attributes.getString("brand"));
			}
			if (attributes.has("model")) {
				channel.put(channelName + ".$.model", attributes.getString("model"));
			}
			if (attributes.has("warranty_type")) {
				channel.put(channelName + ".$.warrantyType", attributes.getString("warranty_type"));
			}
			if (SKUDetails.has("special_price") && !SKUDetails.getString("special_price").equals("0.0")) {
				double salePriceStr = Double.parseDouble(SKUDetails.getString("special_price"));
				long saleAmount = (long) (salePriceStr * 100);
				channel.put(channelName + ".salePrice",
						CurrencyUtil.getAmountObject(saleAmount, exchange.getProperty("currencyCode", String.class)));
				channel.put(channelName + ".saleStartDate",
						DateUtil.getSIADateFormatWithInput(SKUDetails.getString("special_from_date") + " 00:00:00"));
				channel.put(channelName + ".saleEndDate",
						DateUtil.getSIADateFormatWithInput(SKUDetails.getString("special_to_date") + " 00:00:00"));
			}
			channel.put(channelName + ".$.nickNameID", exchange.getProperty("nickNameID", String.class));
			channel.put(channelName + ".$.timeLastUpdated", System.currentTimeMillis() / 1000L);
			channel.put(channelName + ".$.refrenceID", SKUDetails.getString("SellerSku"));
			channel.put("refrenceID", SKUDetails.getString("SellerSku"));
			if (exchange.getProperties().containsKey("itemSpecifics")) {
				channel.put(channelName + ".$.itemSpecifics", exchange.getProperty("itemSpecifics"));
			}
			/*
			 * if (!SIAImages.get(SKU).isEmpty()) { BasicDBList imageURIList =
			 * getImageURIs(SIAImages.get(SKU), SKU); inventory.put("imageURI",
			 * imageURIList); channel.put("imageURI", imageURIList); }
			 */
			log.debug("Update Linked Data " + channel);
			updateList.add(channel);
		}
		exchange.getOut().setBody(updateList);
	}

}