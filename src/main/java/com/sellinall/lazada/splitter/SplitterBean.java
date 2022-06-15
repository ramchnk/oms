/**
 * 
 */
package com.sellinall.lazada.splitter;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Body;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultMessage;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;

/**
 * @author vikraman
 * 
 */
public class SplitterBean {
	static Logger log = Logger.getLogger(SplitterBean.class.getName());
	
	@SuppressWarnings("unchecked")
	public List<Message> splitInventory(@Body BasicDBObject body) {
		ArrayList<BasicDBObject> channelInventory = (ArrayList<BasicDBObject>) body.get("lazada");
		List<Message> answer = new ArrayList<Message>();
		for (BasicDBObject channel : channelInventory) {
			BasicDBObject splitBody = new BasicDBObject(body.toMap());
			splitBody.put("lazada", channel);
			DefaultMessage message = new DefaultMessage();
			message.setBody(splitBody);
			answer.add(message);
		}

		return answer;
	}

	@SuppressWarnings("unchecked")
	public List<Message> splitUser(@Body BasicDBObject body) {
		ArrayList<BasicDBObject> channelUser = (ArrayList<BasicDBObject>) body.get("lazada");
		List<Message> answer = new ArrayList<Message>();
		for (BasicDBObject channel : channelUser) {
			if (isAccountStatusActive(channel)) {
				BasicDBObject splitBody = new BasicDBObject(body.toMap());
				splitBody.put("lazada", channel);
				DefaultMessage message = new DefaultMessage();
				message.setBody(splitBody);
				answer.add(message);
			}
		}
		return answer;
	}

	public boolean isAccountStatusActive(BasicDBObject channel) {
		if (!channel.containsField("status")) {
			return true;
		}
		if (!channel.getString("status").equals("X")) {
			return true;
		}
		return false;
	}

	// For Remove item Array call
	public List<Message> splitSKU(@Body JSONObject inBody) throws JSONException {
		log.debug("iniside SKU SplitterBean");
		JSONArray skuList = inBody.getJSONArray("SKUList");
		List<Message> answer = new ArrayList<Message>();
		for (int i = 0; i < skuList.length(); i++) {
			JSONObject splitBody = new JSONObject();
			DefaultMessage message = new DefaultMessage();
			JSONObject skuData = skuList.getJSONObject(i);
			splitBody.put("accountNumber", inBody.getString("accountNumber"));
			splitBody.put("SKU", skuData.getString("SKU"));
			splitBody.put("siteNicknames", skuData.get("siteNicknames"));
			splitBody.put("needToRemoveParent", skuData.getString("needToRemoveParent"));
			splitBody.put("requestType", inBody.getString("requestType"));
			splitBody.put("channel", inBody.getString("site"));
			message.setBody(splitBody);
			answer.add(message);
		}
		return answer;
	}
	
	public List<Message> splitNickNames(@Body JSONObject inBody) throws JSONException {
		log.debug("iniside nickName SplitterBean");
		JSONArray nickNames = inBody.getJSONArray("siteNicknames");
		List<Message> answer = new ArrayList<Message>();
		for (int i = 0; i < nickNames.length(); i++) {
			JSONObject splitBody = new JSONObject();
			DefaultMessage message = new DefaultMessage();
			splitBody.put("nickNameID", nickNames.getString(i));
			message.setBody(splitBody);
			answer.add(message);
		}
		return answer;
	}	
	
	public List<Message> splitRefrenceIDByLimit(@Body ArrayList<BasicDBObject> inBody) {
		int defualtSplitterSize = 10;
		List<Message> answer = new ArrayList<Message>();
		int size = inBody.size();
		int splitterSize = size / defualtSplitterSize;
		int i;
		for (i = 0; i < splitterSize; i++) {
			DefaultMessage message = new DefaultMessage();
			List list = new ArrayList<BasicDBObject>(
					inBody.subList(i * defualtSplitterSize, (i * defualtSplitterSize) + defualtSplitterSize));
			message.setBody(list);
			answer.add(message);
		}
		if (size % 10 != 0) {
			DefaultMessage message = new DefaultMessage();
			List list = new ArrayList<BasicDBObject>(
					inBody.subList(i * defualtSplitterSize, (i * defualtSplitterSize) + size % 10));
			message.setBody(list);
			answer.add(message);
		}	
		return answer;
	}
	
}