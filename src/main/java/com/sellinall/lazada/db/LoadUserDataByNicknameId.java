/**
 * 
 */
package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.config.APIUrlConfig;
import com.mudra.sellinall.config.Config;
import com.mudra.sellinall.database.DbUtilities;

/**
 * @author vikraman
 * 
 */
public class LoadUserDataByNicknameId implements Processor {
	static Logger log = Logger.getLogger(LoadUserDataByNicknameId.class);

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		String nickNameId = exchange.getProperty("nickNameID", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		DBObject queryResult = runQuery(accountNumber, nickNameId);
		String hostURL ="";
		String channelName = nickNameId.split("-")[0];
		exchange.setProperty("channelName", channelName);
		List<BasicDBObject> channelList = (List<BasicDBObject>) queryResult.get("lazada");
		exchange.setProperty("autoMatch", queryResult.get("autoMatch"));
		BasicDBObject channelObj = channelList.get(0);
		exchange.setProperty("userChannel", channelObj);
		queryResult.put(channelName, channelObj);
		exchange.setProperty("UserDetails", queryResult);
		exchange.setProperty("merchantID", queryResult.get("merchantID"));
		String countryCode = channelObj.getString("countryCode");
		boolean isGlobalAccount = false;
		if (countryCode.equalsIgnoreCase("global")) {
			isGlobalAccount = true;
		} else {
			exchange.setProperty("currencyCode", channelObj.getString("currencyCode"));
			exchange.setProperty("countryCode", countryCode);
		}
		exchange.setProperty("isGlobalAccount", isGlobalAccount);
		if (channelObj.containsField("importRecordsLimit")) {
			exchange.setProperty("importRecordsLimit", channelObj.getInt("importRecordsLimit"));
		}
		if (channelObj.containsField("wms")) {
			ArrayList<String> wms = (ArrayList<String>) channelObj.get("wms");
			exchange.setProperty("warehouseIDList", wms);
		}
		boolean getAccessToken=false;
		if(channelObj.containsField("postHelper")) {
			BasicDBObject postHelper = (BasicDBObject) channelObj.get("postHelper");
			if (postHelper.containsField("chatRefreshToken") && !postHelper.getString("chatRefreshToken").isEmpty()) {
				exchange.setProperty("chatRefreshToken", postHelper.getString("chatRefreshToken"));
			}
			if (!isGlobalAccount) {
				hostURL = APIUrlConfig.getNewAPIUrl(countryCode.toUpperCase());
			}
			exchange.setProperty("refreshToken", postHelper.getString("refreshToken"));
			getAccessToken = true;
			if (exchange.getProperties().containsKey("requestType")
					&& (exchange.getProperty("requestType").equals("pullInventory")
							|| exchange.getProperty("requestType").equals("processPullInventoryByPage"))
					&& exchange.getProperties().containsKey("importCountry")) {
				countryCode = exchange.getProperty("importCountry", String.class);
				exchange.setProperty("countryCode", countryCode);
				exchange.setProperty("currencyCode", Config.getConfig().getCurrencyCode(countryCode.toUpperCase()));
				hostURL = APIUrlConfig.getNewAPIUrl(countryCode.toUpperCase());
			}
		} else {
			log.error("postHelper not found for this account: " + exchange.getProperty("accountNumber")
					+ " nickNameId: " + nickNameId);
		}
		exchange.setProperty("hostURL", hostURL);
		exchange.setProperty("getAccessToken", getAccessToken);
		boolean individualSKUPerChannel = false;
		boolean isIndividualSKUPerChannelEnabled = Config.getConfig().isIndividualSKUPerChannelEnabled();
		if (queryResult.containsField("individualSKUPerChannel") && isIndividualSKUPerChannelEnabled) {
			individualSKUPerChannel = (Boolean) queryResult.get("individualSKUPerChannel");
		}
		exchange.setProperty("individualSKUPerChannel", individualSKUPerChannel);
		boolean isAutoStatusUpdate = false;
		if (channelObj.containsField("isAutoStatusUpdate")) {
			isAutoStatusUpdate = channelObj.getBoolean("isAutoStatusUpdate");
		}
		exchange.setProperty("isAutoStatusUpdate", isAutoStatusUpdate);
		// if isPartialAutoStatusUpdate flag is false,
		// will deactivate the product when last child of the product stock level reach zero count.
		boolean isPartialAutoStatusUpdate = true;
		if (channelObj.containsField("isPartialAutoStatusUpdate")) {
			isPartialAutoStatusUpdate = channelObj.getBoolean("isPartialAutoStatusUpdate");
		}
		exchange.setProperty("isPartialAutoStatusUpdate", isPartialAutoStatusUpdate);
		if (channelObj.containsField("isEncodeShippingLabel") && channelObj.getBoolean("isEncodeShippingLabel")) {
			exchange.setProperty("isEncodeShippingLabel", true);
		}
		exchange.setProperty("isAutoPackOrders", false);
		if (channelObj.containsField("isAutoPackOrders") && channelObj.getBoolean("isAutoPackOrders")) {
			List<BasicDBObject> shippingServiceProviderList = new ArrayList<BasicDBObject>();
			String preferredLogistic = "";
			if (channelObj.containsField("shippingServiceProvider")) {
				shippingServiceProviderList = (List<BasicDBObject>) channelObj.get("shippingServiceProvider");
			}
			for (BasicDBObject shippingServiceProvider : shippingServiceProviderList) {
				if (shippingServiceProvider.containsField("preferred")
						&& shippingServiceProvider.getBoolean("preferred")) {
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
						+ exchange.getProperty("accountNumber") + " nickNameId: " + nickNameId);
			}
		}
		if (channelObj.containsField("shippingCarrier")) {
			List<String> shippingCarrierList = (ArrayList<String>) channelObj.get("shippingCarrier");
			for (String shippingCarrier : shippingCarrierList) {
				if (!shippingCarrier.equals("none")) {
					exchange.setProperty("isSellerOwnFleet", true);
					break;
				}
			}
		}
		boolean processOrdersWithSKUOnly = false;
		if(channelObj.containsField("processOrdersWithSKUOnly")){
			processOrdersWithSKUOnly = channelObj.getBoolean("processOrdersWithSKUOnly");
		}
		exchange.setProperty("processOrdersWithSKUOnly", processOrdersWithSKUOnly);
	}

	private DBObject runQuery(String accountNumber, String nickNameId) {
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("nickName.id", nickNameId);
		BasicDBObject channel = new BasicDBObject("$elemMatch", elemMatch);
		BasicDBObject searchQuery = new BasicDBObject();
		ObjectId objId = new ObjectId(accountNumber);
		searchQuery.put("_id", objId);
		searchQuery.put("lazada", channel);

		BasicDBObject projection = new BasicDBObject("lazada.$", 1);
		projection.put("merchantID", 1);
		projection.put("autoMatch", 1);
		projection.put("individualSKUPerChannel", 1);
		projection.put("wmsList", 1);
		projection.put("isAutoStatusUpdate", 1);

		DBCollection table = DbUtilities.getDBCollection("accounts");
		BasicDBObject channelObj = (BasicDBObject) table.findOne(searchQuery, projection);
		return channelObj;
	}
}