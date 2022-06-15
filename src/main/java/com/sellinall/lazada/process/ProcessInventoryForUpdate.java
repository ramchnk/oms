package com.sellinall.lazada.process;

import java.util.ArrayList;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

/**
 * @author Ramchdanran.K
 *
 */
public class ProcessInventoryForUpdate implements Processor {
	static Logger log = Logger.getLogger(ProcessInventoryForUpdate.class.getName());

	public void process(Exchange exchange) throws Exception {
		ArrayList<BasicDBObject> inventoryList = (ArrayList<BasicDBObject>) exchange.getIn().getBody();
		if (inventoryList.size() == 0 && exchange.getProperties().containsKey("isRequestFromBatch")
				&& exchange.getProperty("isRequestFromBatch", Boolean.class)) {
			exchange.setProperty("failureReason", "SKU not found");
			return;
		}
		if (exchange.getProperty("isStatusUpdate", boolean.class)) {
			if (exchange.getProperties().containsKey("siteNicknames")) {
				List<String> siteNicknameList = (List<String>) JSON.parse(exchange.getProperty("siteNicknames").toString());
				String nickNameId = siteNicknameList.get(0);
				exchange.setProperty("nickNameID", nickNameId);
				BasicDBObject inventory = (BasicDBObject) inventoryList.get(0);
				processInventory(inventory, siteNicknameList);
			}
			exchange.setProperty("inventoryDetails", inventoryList);
			return;
		}
		if (exchange.getProperty("isImagesUpdate", boolean.class)) {
			for (BasicDBObject inventory : inventoryList) {
				String SKU = (String) inventory.get("SKU");
				List<BasicDBObject> lazada = ((List<BasicDBObject>) inventory.get("lazada"));
				inventory.put("lazada", lazada.get(0));
				if(!SKU.contains("-")) {
					exchange.setProperty("inventory", inventory);
					exchange.getOut().setBody(inventory);
				}
			}
			exchange.setProperty("inventoryDetails", inventoryList);
		}
		BasicDBObject inventory = getParentInventory(inventoryList);
		boolean isVariantParent = false;
		if(inventory.get("lazada") instanceof BasicDBObject) {
			BasicDBObject lazadaObj = (BasicDBObject) inventory.get("lazada");
			if (lazadaObj.containsField("variants")) {
				isVariantParent = true;
			}
		} else {
			List<BasicDBObject> lazada = (List<BasicDBObject>) inventory.get("lazada");
			BasicDBObject lazadaObj = lazada.get(0);
			if (lazadaObj.containsField("variants")) {
				isVariantParent = true;
			}
		}
		exchange.setProperty("isVariantParent", isVariantParent);
		exchange.setProperty("inventory", inventory);
		exchange.getOut().setBody(inventory);
	}

	private BasicDBObject getParentInventory(List<BasicDBObject> inventoryList) throws JSONException {
		for (BasicDBObject inventory : inventoryList) {
			if (inventory.containsField("variants")) {
				return inventory;
			}
		}
		return inventoryList.get(0);
	}

	private void processInventory(BasicDBObject inventory, List<String> siteNicknameList) {
		ArrayList<BasicDBObject> channelList = (ArrayList<BasicDBObject>) inventory.get("lazada");
		ArrayList<BasicDBObject> newChannelList = new ArrayList<BasicDBObject>();
		for (BasicDBObject channel : channelList) {
			if (siteNicknameList.contains(channel.getString("nickNameID"))) {
				newChannelList.add(channel);
			}
		}
		inventory.put("lazada", newChannelList);
	}
}
