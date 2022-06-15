/**
 * 
 */
package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Senthil
 * 
 */
public class InitializeFeedType implements Processor {
	static Logger log = Logger.getLogger(InitializeFeedType.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		log.debug("Feed check Message Received "+inBody.toString());
		exchange.setProperty("accountNumber", inBody.getString("accountNumber"));
		exchange.setProperty("channelName", inBody.getString("channelName"));
		exchange.setProperty("FeedID", inBody.getString("FeedID"));
		exchange.setProperty("Action", inBody.getString("Action"));
		exchange.setProperty("requestType", inBody.getString("feedRequestType"));
		JSONObject inventory = inBody.getJSONObject("inventory");
		exchange.setProperty("inventory", inventory);
		// User Id and Api we have added to inventory at the time of message
		// publish
		// It is not presented in inventory table
		exchange.setProperty("nickNameID", getNickNameId(inventory, inBody.getString("channelName")));
		exchange.setProperty("channelUserId", inventory.getString("channelUserId"));
		exchange.setProperty("channelapiKey", inventory.getString("channelapiKey"));
		exchange.setProperty("channelHostUrl", inventory.getString("channelHostUrl"));
		if(inBody.has("batchAddItemSKUMap")){
			exchange.setProperty("SKUMap", inBody.get("batchAddItemSKUMap"));
		}
	}

	private String getNickNameId(JSONObject inventory, String channelName) throws JSONException {
		JSONObject channel = inventory.getJSONObject(channelName);
		return channel.getString("nickNameID");
	}

}