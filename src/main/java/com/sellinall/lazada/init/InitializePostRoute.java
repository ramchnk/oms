/**
 * 
 */
package com.sellinall.lazada.init;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mudra.sellinall.config.APIUrlConfig;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.ContentChangesUtil;
import com.sellinall.util.DateUtil;
import com.sellinall.util.enums.SIAInventoryStatus;

/**
 * @author Ramachandran.K
 * 
 */
public class InitializePostRoute implements Processor {
	static Logger log = Logger.getLogger(InitializePostRoute.class.getName());
	private static long ONE_DAY_IN_SECONDS = 86400;

	public void process(Exchange exchange) throws Exception {
		BasicDBObject inventory = exchange.getIn().getBody(BasicDBObject.class);
		BasicDBObject channel = (BasicDBObject) inventory.get("lazada");
		String nickNameID = channel.getString("nickNameID");
		exchange.setProperty("nickNameID", nickNameID);
		log.debug("InitializePostRoute inbody: " + inventory);
		processUserDetails(exchange);
		processPrePostData(exchange);
		exchange.getOut().setBody(inventory);
	}

	private void processUserDetails(Exchange exchange) {
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		BasicDBObject userDetails = exchange.getProperty("UserDetails", BasicDBObject.class);
		ArrayList<BasicDBObject> channelList = (ArrayList<BasicDBObject>) userDetails.get("lazada");
		boolean getAccessToken = false;
		for (BasicDBObject channel : channelList) {
			BasicDBObject nickName = (BasicDBObject) channel.get("nickName");
			if (nickName.getString("id").equals(nickNameID)) {
				String hostUrl = "";
				exchange.setProperty("countryCode", channel.getString("countryCode").toUpperCase());
				if(channel.containsField("postHelper")) {
					hostUrl = APIUrlConfig.getNewAPIUrl(channel.getString("countryCode").toUpperCase());
					BasicDBObject postHelper = (BasicDBObject) channel.get("postHelper");
					exchange.setProperty("refreshToken", postHelper.getString("refreshToken"));
					getAccessToken = true;
				} else {
					log.error("postHelper not found for this account: " + exchange.getProperty("accountNumber")
							+ " nickNameID : " + nickNameID);
				}
				exchange.setProperty("hostURL", hostUrl);
				break;
			}
		}
		String timeZoneOffset = LazadaUtil.timeZoneCountryMap
				.get(exchange.getProperty("countryCode", String.class));
		exchange.setProperty("timeZoneOffset", timeZoneOffset);
		exchange.setProperty("getAccessToken", getAccessToken);
	}

	private void processPrePostData(Exchange exchange) throws IOException, JSONException {
		BasicDBObject inventory = exchange.getProperty("inventory", BasicDBObject.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String requestType = exchange.getProperty("requestType", String.class);
		if (requestType.equals("updateItem") || requestType.equals("removeArrayItem")) {
			boolean hasVariants = false;
			boolean isUpdateSellableStock = false, isUpdateQuantityByDiff = false;
			inventory = exchange.getIn().getBody(BasicDBObject.class);
			BasicDBObject lazadaObj = (BasicDBObject) inventory.get("lazada");
			if (inventory.getString("SKU").contains("-") || lazadaObj.containsField("variants")) {
				hasVariants = true;
			}
			exchange.setProperty("inventory", inventory);
			if (inventory.containsField("customSKU") && exchange.getProperties().containsKey("fieldsToUpdate")) {
				ArrayList<String> fieldsToUpdate = (ArrayList<String>) exchange.getProperty("fieldsToUpdate");
				if (fieldsToUpdate.contains("quantity") || fieldsToUpdate.contains("bufferQuantity")) {
					if (Config.getConfig().isUpdateStockViaSellableQuantityApi() && !fieldsToUpdate.contains("salePrice")
							&& !fieldsToUpdate.contains("price") && !fieldsToUpdate.contains("images")) {
						isUpdateSellableStock = true;
					}

					String customSKU = inventory.getString("customSKU");
					exchange.setProperty("sellerSKU", customSKU);
					String countryCode = exchange.getProperty("countryCode", String.class);
					String promotionStartDateStr = Config.getConfig().getPromotionStartDate();
					String promotionEndDateStr = Config.getConfig().getPromotionEndDate();
					long promotionStartDate = DateUtil.getUnixTimestamp(promotionStartDateStr, "dd-MM-yyyy HH:mm:ss",
							LazadaUtil.timeZoneCountryMap.get(countryCode));
					long promotionEndDate = DateUtil.getUnixTimestamp(promotionEndDateStr, "dd-MM-yyyy HH:mm:ss",
							LazadaUtil.timeZoneCountryMap.get(countryCode));
					long currentTime = System.currentTimeMillis() / 1000L;

					if (isUpdateSellableStock && promotionStartDate < currentTime && currentTime < promotionEndDate) {
						isUpdateQuantityByDiff = true;
					}

					BasicDBObject lazada = (BasicDBObject) inventory.get("lazada");
					int qtyUnderProcessing = 0, sellableStockFromLazada = 0;
					if (!isUpdateSellableStock || isUpdateQuantityByDiff) {
						JSONObject itemResponse = LazadaUtil.getItemDetailsBySellerSKU(exchange);
						if (itemResponse.has("multiWarehouseInventories")
								&& itemResponse.getJSONArray("multiWarehouseInventories").length() == 1) {
							JSONObject stockDetails = itemResponse.getJSONArray("multiWarehouseInventories")
									.getJSONObject(0);
							if (isUpdateQuantityByDiff) {
								if (stockDetails.has("sellableQuantity")) {
									sellableStockFromLazada = stockDetails.getInt("sellableQuantity");
								} else {
									sellableStockFromLazada = stockDetails.getInt("quantity");
								}
							} else {
								if (stockDetails.has("occupyQuantity")) {
									qtyUnderProcessing = stockDetails.getInt("occupyQuantity");
								}
								if (stockDetails.has("withholdQuantity")) {
									qtyUnderProcessing += stockDetails.getInt("withholdQuantity");
								}
							}
						} else {
							if (itemResponse.has("multiWarehouseInventories")
									&& itemResponse.getJSONArray("multiWarehouseInventories").length() > 1) {
								log.warn("Multiple warehouse found for accountNumber:" + accountNumber
										+ ",for nickNameID:" + nickNameID + ",for customSKU:" + customSKU
										+ " and warehouse list is :" + itemResponse.get("multiWarehouseInventories"));
							}
							/*
							 * Note : If error response in get product api or found multiple warehouse, then
							 * we will not call adjust sellable quantity api
							 */
							isUpdateQuantityByDiff = false;
							qtyUnderProcessing = LazadaUtil.loadSoldCount(accountNumber, customSKU, nickNameID);
						}
					}
					exchange.setProperty("qtyUnderProcessing", qtyUnderProcessing);
					int overAllQuantity = 0;
					// parent inventory doesn't  have noOfItem field, so added check here
					if (lazada.containsField("noOfItem")) {
						overAllQuantity = lazada.getInt("noOfItem");
					}
					overAllQuantity += qtyUnderProcessing;
					exchange.setProperty("quantityDiff", overAllQuantity - sellableStockFromLazada);
					exchange.setProperty("isUpdateQuantityByDiff", isUpdateQuantityByDiff);
					log.info("Quantity update request for quantityChange with accountNumber : "
							+ exchange.getProperty("accountNumber", String.class) + " , nickNameID : "
							+ exchange.getProperty("nickNameID", String.class) + " , SKU : " + inventory.getString("SKU")
							+ " and the quantity is : " + overAllQuantity);
				}
			}
			exchange.setProperty("isUpdateSellableStock", isUpdateSellableStock);
			exchange.setProperty("hasVariants", hasVariants);
		}
		BasicDBObject lazada = (BasicDBObject) inventory.get("lazada");
		exchange.setProperty("itemStatus", lazada.getString("status"));
		exchange.setProperty("nickNameID", lazada.getString("nickNameID"));

		if (exchange.getProperties().containsKey("hasSalePrice")
				&& exchange.getProperty("hasSalePrice", Boolean.class)) {
			exchange.setProperty("isSalePriceUpdate", true);
		} else if (exchange.getProperties().containsKey("hasSalePrice")
				&& !exchange.getProperty("hasSalePrice", Boolean.class) && lazada.containsField("isPromotionEnabled")
				&& lazada.getBoolean("isPromotionEnabled")) {
			exchange.setProperty("isSalePriceRemove", true);
			long timeInSecs = System.currentTimeMillis() / 1000 - ONE_DAY_IN_SECONDS;
			exchange.setProperty("yesterdayInSeconds", timeInSecs);
		}
		if (!exchange.getProperty("isStatusUpdate", boolean.class)) {
			processDescription(exchange, inventory);
		}
	}
	
	private void processDescription(Exchange exchange, BasicDBObject inventory) {
		BasicDBObject lazada = (BasicDBObject) inventory.get("lazada");
		String description = "";
		String descriptionEnglish = "";
		String inventoryDescription = inventory.getString("itemDescription");
		String inventoryDescriptionEnglish = inventory.getString("itemDescriptionEnglish");
		if (lazada.containsField("itemDescription")) {
			description = lazada.getString("itemDescription");
		} else if (inventory.containsField("itemDescription")){
			description = inventory.getString("itemDescription");
		}
		if (lazada.containsField("itemDescriptionEnglish")) {
			descriptionEnglish = lazada.getString("itemDescriptionEnglish");
		}
		exchange.setProperty("itemDescription", parseDescription(exchange, description, inventoryDescription, lazada));
		exchange.setProperty("itemDescriptionEnglish", parseDescription(exchange, descriptionEnglish, inventoryDescriptionEnglish, lazada));
		if (lazada.containsField("shortDescription")) {
			exchange.setProperty("shortDescription", parseShortDescription(lazada.getString("shortDescription")));
		}
		if (lazada.containsField("shortDescriptionEnglish")) {
			exchange.setProperty("shortDescriptionEnglish",
					parseShortDescription(lazada.getString("shortDescriptionEnglish")));
		}
	}
	
	private Object parseShortDescription(String shortDescription) {
		shortDescription  = ContentChangesUtil.nl2br(shortDescription).replace("\t", "");
		shortDescription = shortDescription.replace("<p>&nbsp;</p>", "");
		shortDescription = shortDescription.replaceAll("[^\\x00-\\x7F\\p{IsThai}\\p{IsLatn}\\p{IsHan}]", "");
		shortDescription = shortDescription.replace("<br>", "");
		shortDescription = shortDescription.replace("&nbsp;", "");
		return shortDescription;
	}

	private String parseDescription(Exchange exchange, String data, String inventoryDescription,
			BasicDBObject inventoryLazada) {
		if (data.startsWith("userTemplate-")) {
			BasicDBObject userDetails = exchange.getProperty("UserDetails", BasicDBObject.class);
			data = LazadaUtil.processItemDescription(inventoryLazada, userDetails, data, inventoryDescription);
			if (data.contains("Failure-")) {
				exchange.setProperty("failureReason", data);
				exchange.setProperty("isPreValidationSuccess", false);
				return null;
			}
			return data;
		}

		// \n to <br>
		data = ContentChangesUtil.nl2br(data).replace("\t", "");
		data = data.replace("<br>", "");
		// replace special char
		data = data.replaceAll("[^\\x00-\\x7F\\p{IsThai}\\p{IsLatn}\\p{IsHan}]", "");
		// Replace unicode
		data = StringEscapeUtils.unescapeJava(data);
		return data;
	}
}