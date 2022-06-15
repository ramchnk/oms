/**
 * 
 */
package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mudra.sellinall.config.APIUrlConfig;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.DateUtil;

/**
 * @author vikraman
 *
 */
public class InitializeProcessOrders implements Processor {
	static Logger log = Logger.getLogger(InitializeProcessOrders.class.getName());
	static final int adjustLastScannedTimeBy = 300;

	public void process(Exchange exchange) throws Exception {
		DBObject user = exchange.getIn().getBody(DBObject.class);
		Boolean isEligibleToPullMissingItem = false;
		if (user.containsField("isEligibleToPullMissingItem")) {
			if (exchange.getProperty("requestType", String.class).equals("scanNewOrders")) {
				isEligibleToPullMissingItem = (Boolean) user.get("isEligibleToPullMissingItem");
			} else if (exchange.getProperty("requestType", String.class).equals("processPendingNotification")
					&& exchange.getProperty("notificationType", String.class).equals("order")) {
				List statuses = exchange.getProperty("statuses", List.class);
				if (statuses.contains("unpaid") || statuses.contains("pending")) {
					isEligibleToPullMissingItem = (Boolean) user.get("isEligibleToPullMissingItem");
				}
			}
		}
		exchange.setProperty("isEligibleToPullMissingItem", isEligibleToPullMissingItem);
		String channelName=exchange.getProperty("channelName",String.class);
		log.debug("InitializeItemSyncMessageListnerRoute Received body: " + user);
		exchange.getOut().setBody(user);
		BasicDBObject channel = (BasicDBObject) user.get(channelName);
		String nickName = ((BasicDBObject) channel.get("nickName")).getString("id");

		String countryCode = "";
		if (exchange.getProperties().containsKey("requestType")
				&& exchange.getProperty("requestType", String.class).equals("processPendingNotification")) {
			countryCode = (String) exchange.getProperty("countryCode");
		} else {
			countryCode = channel.getString("countryCode");
		}

		String hostURL =  "";
		if (channel.containsField("postHelper")) {
			BasicDBObject postHelper = (BasicDBObject) channel.get("postHelper");
			exchange.setProperty("refreshToken", postHelper.getString("refreshToken"));
			hostURL = APIUrlConfig.getConfig().getNewAPIUrl(countryCode);
			exchange.setProperty("getAccessToken", true);
		} else {
			log.error("postHelper not found for this account: " + exchange.getProperty("accountNumber")
					+ " nickNameId: " + nickName);
		}
		if (channel.containsField("isEncodeShippingLabel") && channel.getBoolean("isEncodeShippingLabel")) {
			exchange.setProperty("isEncodeShippingLabel", true);
		}
		exchange.setProperty("isAutoPackOrders", false);
		if (channel.containsField("isAutoPackOrders") && channel.getBoolean("isAutoPackOrders")) {
			List<BasicDBObject> shippingServiceProviderList = new ArrayList<BasicDBObject>();
			String preferredLogistic = "";
			if (channel.getString("countryCode").equals("GLOBAL")) {
				if (channel.containsKey("shippingServiceProvider") && !countryCode.equals("GLOBAL")) {
					BasicDBObject shippingServiceProvider = (BasicDBObject) channel.get("shippingServiceProvider");
					if (shippingServiceProvider.containsField(countryCode)) {
						shippingServiceProviderList = (List<BasicDBObject>) shippingServiceProvider.get(countryCode);
					}
				}
			} else if (channel.containsField("shippingServiceProvider")) {
				shippingServiceProviderList = (List<BasicDBObject>) channel.get("shippingServiceProvider");
			}
			for (BasicDBObject shippingServiceProvider : shippingServiceProviderList) {
				if (shippingServiceProvider.containsField("preferred") && shippingServiceProvider.getBoolean("preferred")) {
					preferredLogistic = shippingServiceProvider.containsField("Name")
							? shippingServiceProvider.getString("Name")
							: shippingServiceProvider.getString("name");
					break;
				}
			}
			if (!preferredLogistic.isEmpty()) {
				exchange.setProperty("isAutoPackOrders", true);
				exchange.setProperty("preferredLogistic", preferredLogistic);
			} else {
				log.error("isAutoPackOrders enable But there is no preferred shippingServiceProvider "
						+ exchange.getProperty("accountNumber") + " nickNameId: " + nickName);
			}
		}
		if (channel.containsField("shippingCarrier")) {
			List<String> shippingCarrierList = (ArrayList<String>) channel.get("shippingCarrier");
			for (String shippingCarrier : shippingCarrierList) {
				if (!shippingCarrier.equals("none")) {
					exchange.setProperty("isSellerOwnFleet", true);
					break;
				}
			}
		}
		long lastScannedTime = 0;
		if (exchange.getProperty("requestType", String.class).equals("scanNewOrders")
				&& channel.containsField("lastNewOrderScannedTime")) {
			lastScannedTime = channel.getLong("lastNewOrderScannedTime") - adjustLastScannedTimeBy;
		} else if (exchange.getProperty("requestType", String.class).equals("scanUpdatedOrders")
				&& channel.containsField("lastUpdatedOrderScannedTime")) {
			lastScannedTime = channel.getLong("lastUpdatedOrderScannedTime") - adjustLastScannedTimeBy;
		} else if (channel.containsField("lastScannedTime")) {
			lastScannedTime = channel.getLong("lastScannedTime") - adjustLastScannedTimeBy;
		}
		String strLastScannedTime = "";
		if (lastScannedTime > 0) {
			strLastScannedTime = DateUtil.getDateFromSIAFormat(lastScannedTime, "yyyy-MM-dd'T'HH:mm:ssXXX",
					LazadaUtil.timeZoneCountryMap.get(countryCode));
		}
		exchange.setProperty("lastScannedTime", strLastScannedTime);
		// current time
		long lastRequestEndTime = System.currentTimeMillis() / 1000L;
		if (exchange.getProperty("requestType", String.class).equals("scanUpdatedOrders")) {
			/*
			 * NOTE: in order to check order notification process, we are not processing
			 * last 5 mins orders in updated order polling
			 */
			lastRequestEndTime = lastRequestEndTime - adjustLastScannedTimeBy;
		}
		// This Property Will use in time DB Update
		exchange.setProperty("lastRequestEndTime", lastRequestEndTime);
		exchange.setProperty("pageOffSet", 0);
		exchange.setProperty("hasMoreRecords", true);
		exchange.setProperty("hostURL", hostURL);
		exchange.setProperty("UserDetails", user);
		exchange.setProperty("nickNameID", getNicknameId(user,channelName));
		exchange.setProperty("merchantID", user.get("merchantID"));
		exchange.setProperty("currencyCode", getCurrencyCode(user, channelName, countryCode));
		exchange.setProperty("countryCode", countryCode);
		exchange.setProperty("channel", channel);
		boolean individualSKUPerChannel = false;
		boolean isIndividualSKUPerChannelEnabled = Config.getConfig().isIndividualSKUPerChannelEnabled();
		if (user.containsField("individualSKUPerChannel") && isIndividualSKUPerChannelEnabled) {
			individualSKUPerChannel = (Boolean) user.get("individualSKUPerChannel");
		}
		exchange.setProperty("individualSKUPerChannel", individualSKUPerChannel);
		// TODO:Have to handle multiplewarehouses case in further,now its a single warehouse per channel
		if (channel.containsField("wms")) {
			List<String> wms = (List<String>) channel.get("wms");
			if (wms.size() > 0) {
				exchange.setProperty("wmsID", wms.get(0));
			}
		}
	}

	private String getNicknameId(DBObject userDetails,String channelName) {
		BasicDBObject channel = (BasicDBObject) userDetails.get(channelName);
		BasicDBObject nickName = (BasicDBObject) channel.get("nickName");
		return nickName.getString("id");
	}
	
	private String getCurrencyCode(DBObject userDetails,String channelName, String countryCode){
		BasicDBObject channel = (BasicDBObject) userDetails.get(channelName);
		if (channel.getString("countryCode").equals("GLOBAL")) {
			return LazadaUtil.countryCodeToCurrencyMap.get(countryCode);
		}
		return channel.getString("currencyCode");
	}
}