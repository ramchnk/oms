package com.sellinall.lazada.db;

import java.util.LinkedHashMap;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.sellinall.util.enums.SIAInventoryStatus;

/**
 * @author Senthil
 * 
 */
public class ProcessInventoryForAutoLinking implements Processor {
	static Logger log = Logger.getLogger(ProcessInventoryForAutoLinking.class.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		String accountNumber = "";
		String existingCustomSKU = "";
		if (exchange.getProperties().containsKey("accountNumber")) {
			accountNumber = (String) exchange.getProperty("accountNumber");
		}
		List<BasicDBObject> queryResults = (List<BasicDBObject>) exchange.getIn().getBody();
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		log.debug("queryResult:" + queryResults + ", for account : " + accountNumber + ", for nickNameID " + nickNameID);
		boolean itemHasVariants = exchange.getProperty("itemHasVariants", Boolean.class);
		String customSKU = null, refrenceID = null;
		boolean individualSKUPerChannel = exchange.getProperty("individualSKUPerChannel", boolean.class);
		if (individualSKUPerChannel) {
			exchange.setProperty("status", SIAInventoryStatus.INITIATED.toString());
		}
		if(queryResults.size()==0){
			exchange.setProperty("itemIsExistInInventoryDetails", false);
			exchange.setProperty("autoLinkFailureReason", "no match found");
			return;
		}
		if(queryResults.get(0).containsField("customSKU") && queryResults.get(0).containsField("variants") && itemHasVariants) {
			existingCustomSKU = queryResults.get(0).getString("customSKU");
			exchange.setProperty("existingCustomSKU", existingCustomSKU);
		}
		if (exchange.getProperties().containsKey("customSKU")) {
			customSKU = exchange.getProperty("customSKU", String.class);
		}
		if (exchange.getProperties().containsKey("refrenceID")) {
			refrenceID = exchange.getProperty("refrenceID", String.class);
		}
		exchange.setProperty("parentSKU", queryResults.get(0).getString("SKU"));
		exchange.setProperty("refrenceIDAndSKUFromDB", new LinkedHashMap<String, Integer>());
		List<String> autoMatch = (List<String>) exchange.getProperty("autoMatch");
		// itemID match
		String itemID = exchange.getProperty("parentItemID", String.class);
		int matchCounter = getItemIDMatches(exchange, queryResults, nickNameID, itemID, itemHasVariants, accountNumber);
		if (matchCounter != 0) {
			return;
		}
		if (!individualSKUPerChannel) {
			// refrenceID match
			matchCounter = getRefrenceIDMatches(exchange, queryResults, nickNameID, refrenceID, itemHasVariants,
					accountNumber);
			if (matchCounter != 0) {
				return;
			}
			// customSKU match
			if (autoMatch != null && autoMatch.contains("customSKU")) {
				matchCounter = getMatches(exchange, queryResults, nickNameID, "customSKU", customSKU, itemHasVariants,
						accountNumber);
				if (matchCounter != 0) {
					return;
				}
			}
		}
		if (matchCounter != 0) {
			log.info("all combination match failed, for account : " + accountNumber);
		} else if (matchCounter == 0) {
			exchange.setProperty("autoLinkFailureReason", "no match found");
		}
	}

	private int getItemIDMatches(Exchange exchange, List<BasicDBObject> queryResults, String nickNameID, String itemID,
			boolean itemHasVariants, String accountNumber) {
		String channelName = exchange.getProperty("channelName", String.class);
		int matchCounter = 0;
		if (itemID == null) {
			return matchCounter;
		}
		BasicDBObject inventory = new BasicDBObject();
		String SKU = null, logSKUs = " ", orphanImageDir = null;
		BasicDBList templateAttributes = null;
		for (BasicDBObject queryResult : queryResults) {
			if (!queryResult.containsField(channelName)) {
				continue;
			}
			List<BasicDBObject> channelList = (List<BasicDBObject>) queryResult.get(channelName);
			for (BasicDBObject channel : channelList) {
				if (channel.getString("nickNameID").equals(nickNameID)) {
					if (channel.containsField("itemID") && channel.getString("itemID").equals(itemID)) {
						SKU = queryResult.getString("SKU");
						inventory = queryResult;
						logSKUs = logSKUs + SKU + " ";
						if (channel.containsField("imageURI")) {
							orphanImageDir = channel.getString("imageURI");
						}
						if (channel.containsField("templateAttributes")) {
							templateAttributes = (BasicDBList) channel.get("templateAttributes");
						}
						matchCounter++;
						exchange.setProperty("inventoryChannel", channel);
					}
				}
			}
		}
		String failureReason = null;
		if (matchCounter == 1) {
			log.info("Match found for itemID " + itemID + ". To be overwritten " + SKU + " " + nickNameID
					+ ", for account : " + accountNumber);
			if (orphanImageDir != null) {
				log.info("potential orphanImageDir analyze for image removal: " + orphanImageDir + ", for account : "
						+ accountNumber);
			}
			if (templateAttributes != null) {
				exchange.setProperty("templateAttributes", templateAttributes);
			}
			exchange.setProperty("itemIsExistInInventoryDetails", true);
			// For uploading image
			exchange.setProperty("inventory", inventory);
			if (itemHasVariants) {
				exchange.setProperty("linkToSKU", SKU.split("-")[0]);
			} else {
				exchange.setProperty("linkToSKU", SKU);
			}
			return matchCounter;
		} else if (matchCounter > 1) {
			failureReason = "more than one documents found with itemID : " + itemID + " " + logSKUs;
			log.error("failure reason , for account : " + accountNumber + " is " + failureReason + ", for nickname: "
					+ nickNameID);
			exchange.setProperty("itemIsExistInInventoryDetails", false);
			exchange.setProperty("autoLinkFailureReason", failureReason);
		} else {
			failureReason = "no match found";
			log.info("no records found with itemID: " + itemID + ", for account : " + accountNumber);
			exchange.setProperty("itemIsExistInInventoryDetails", false);
			exchange.setProperty("autoLinkFailureReason", failureReason);
		}
		return matchCounter;
	}

	@SuppressWarnings("unchecked")
	private int getRefrenceIDMatches(Exchange exchange, List<BasicDBObject> queryResults, String nickNameID,
			String refrenceID, boolean itemHasVariants, String accountNumber) {
		String channelName=exchange.getProperty("channelName",String.class);
		int matchCounter = 0;
		if (refrenceID == null) {
			return matchCounter;
		}
		BasicDBObject inventory=new BasicDBObject();
		String SKU = null, logSKUs = " ", orphanImageDir = null;
		BasicDBList templateAttributes = null;
		for (BasicDBObject queryResult : queryResults) {
			if (!queryResult.containsField(channelName)) {
				continue;
			}
			List<BasicDBObject> channelList = (List<BasicDBObject>) queryResult.get(channelName);
			for (BasicDBObject channel : channelList) {
				if (channel.getString("nickNameID").equals(nickNameID)) {
					if (channel.containsField("refrenceID") && channel.getString("refrenceID").equals(refrenceID)) {
						SKU = queryResult.getString("SKU");
						inventory=queryResult;
						logSKUs = logSKUs + SKU + " ";
						if (channel.containsField("imageURI")) {
							orphanImageDir = channel.getString("imageURI");
						}
						if (channel.containsField("templateAttributes")) {
							templateAttributes = (BasicDBList) channel.get("templateAttributes");
						}
						matchCounter++;
						exchange.setProperty("inventoryChannel", channel);
					}
				}
			}
		}
		String failureReason = null;
		if (matchCounter == 1) {
			log.info("Match found for refrenceID " + refrenceID + ". To be overwritten " + SKU + " " + nickNameID
					+ ", for account : " + accountNumber);
			if (orphanImageDir != null) {
				log.info("potential orphanImageDir analyze for image removal: " + orphanImageDir + ", for account : "
						+ accountNumber);
			}
			if (templateAttributes != null) {
				exchange.setProperty("templateAttributes", templateAttributes);
			}
			exchange.setProperty("itemIsExistInInventoryDetails", true);
			//For uploading image
			exchange.setProperty("inventory", inventory);
			if (itemHasVariants) {
				exchange.setProperty("linkToSKU", SKU.split("-")[0]);
			} else {
				exchange.setProperty("linkToSKU", SKU);
			}
			return matchCounter;
		} else if (matchCounter > 1) {
			failureReason = "more than one documents found with refrenceID: " + refrenceID + " " + logSKUs; 
			log.error("failure reason , for account : " + accountNumber + " is " + failureReason + ", for nickname: "
					+ nickNameID);
			exchange.setProperty("itemIsExistInInventoryDetails", false);
			exchange.setProperty("autoLinkFailureReason", failureReason);
		} else {
			failureReason = "no match found";
			log.info("no records found with refrenceID: " + refrenceID+", for account : " + accountNumber);
			exchange.setProperty("itemIsExistInInventoryDetails", false);
			exchange.setProperty("autoLinkFailureReason", failureReason);
		}
		return matchCounter;
	}

	@SuppressWarnings("unchecked")
	private int getMatches(Exchange exchange, List<BasicDBObject> queryResults, String nickNameID, String matchKey,
			String matchValue, boolean itemHasVariants, String accountNumber) {
		String channelName=exchange.getProperty("channelName",String.class);
		int matchCounter = 0;
		if (matchValue == null) {
			return matchCounter;
		}
		BasicDBObject matchedResult = null;
		for (BasicDBObject queryResult : queryResults) {
			String dbValue = (String) queryResult.get(matchKey);
			if (dbValue != null && dbValue.equals(matchValue)) {
				matchedResult = queryResult;
				matchCounter++;
			}
		}
		String failureReason = null;
		if (matchCounter > 1) {
			failureReason = "more than one documents found with " + matchKey + ": " + matchValue;
			log.error("failure reason , for account : " + accountNumber + " is " + failureReason + ", for nickname: "
					+ nickNameID);
			exchange.setProperty("autoLinkFailureReason", failureReason);
			exchange.setProperty("itemIsExistInInventoryDetails", false);
			return matchCounter;
		} else if (matchCounter == 0) {
			failureReason = "no records found with " + matchKey + ": " + matchValue;
			log.info("failure reason , for account : " + accountNumber + " is " + failureReason + ", for nickname: "
					+ nickNameID);
			exchange.setProperty("autoLinkFailureReason", failureReason);
			exchange.setProperty("itemIsExistInInventoryDetails", false);
			return matchCounter;
		}

		// we know exactly one queryResult
		String SKU = matchedResult.getString("SKU");		
		if (!matchedResult.containsField(channelName)) {
			log.info("Match found with no " + channelName + " object " + matchKey + ": " + matchValue + ", SKU : " + SKU
					+ ", for account : " + accountNumber);
			if (SKU.contains("-")) {
				failureReason = "Inventory matched with child record, matched child SKU : " + SKU;
				exchange.setProperty("itemIsExistInInventoryDetails", false);
				exchange.setProperty("autoLinkFailureReason", failureReason);
			} else {
				exchange.setProperty("itemIsExistInInventoryDetails", true);
				exchange.setProperty("linkToSKU", SKU.split("-")[0]);
			}
			exchange.setProperty("inventory", matchedResult);
			return matchCounter++;
		}
		List<BasicDBObject> channelList = (List<BasicDBObject>) matchedResult.get(channelName);
		for (BasicDBObject channel : channelList) {
			if (channel.getString("nickNameID").equals(nickNameID)
					&& SIAInventoryStatus.ACTIVE.equalsName(channel.getString("status"))) {
				failureReason = "already active inventory record exists with " + matchKey + " "+channelName+" itemID: "
						+ channel.getString("refrenceID") + " SKU: " + SKU;
				exchange.setProperty("itemIsExistInInventoryDetails", false);
				exchange.setProperty("autoLinkFailureReason", failureReason);
				return matchCounter++;
			}
		}
		exchange.setProperty("itemIsExistInInventoryDetails", true);
		//For image upload
		exchange.setProperty("inventory", matchedResult);
		if (itemHasVariants) {
			exchange.setProperty("linkToSKU", SKU.split("-")[0]);
		} else {
			exchange.setProperty("linkToSKU", SKU);
		}
		return matchCounter++;
	}
}