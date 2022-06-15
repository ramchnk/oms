package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;

import com.mongodb.BasicDBObject;

public class ProcessReferenceIDResult implements Processor {
	static Logger log = Logger.getLogger(ProcessReferenceIDResult.class.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		ArrayList<BasicDBObject> inventoryList = (ArrayList<BasicDBObject>) exchange.getIn().getBody();
		if (inventoryList.isEmpty()) {
			log.info(
					"Inventory record doesn't exists for this OrderID: " + exchange.getProperty("orderID", String.class)
							+ " with referenceId list : " + exchange.getProperty("refrenceIDList"));
			Map<String, String> itemIdMap = (Map<String, String>) exchange.getProperty("itemIdMap");
			setItemIDList(exchange, itemIdMap);
			return;
		}
		setSKUMap(exchange, inventoryList);
	}

	private void setItemIDList(Exchange exchange, Map<String, String> itemIdMap) {
		exchange.setProperty("missingItemIdList", new ArrayList<String>(itemIdMap.values()));
		exchange.removeProperty("itemIdMap");
	}

	@SuppressWarnings("unchecked")
	private void setSKUMap(Exchange exchange, ArrayList<BasicDBObject> inventoryList) {
		HashMap<String, String> skuMap = new HashMap<String, String>();
		HashMap<String, String> weightMap = new HashMap<String, String>();
		Map<String, String> itemIdMap = (Map<String, String>) exchange.getProperty("itemIdMap");
		String channelName = exchange.getProperty("channelName", String.class);
		for (BasicDBObject inventory : inventoryList) {
			ArrayList<BasicDBObject> channel = (ArrayList<BasicDBObject>) inventory.get(channelName);
			// Inventory always return single channel object (search query used
			// nick name id)
			skuMap.put(channel.get(0).getString("refrenceID"), inventory.getString("SKU"));
			if (channel.get(0).getString("packageWeight") != null) {
				weightMap.put(channel.get(0).getString("refrenceID"), channel.get(0).getString("packageWeight"));
			} else {
				log.error("packageWeight is missing for SKU : " + inventory.getString("SKU"));
			}
			itemIdMap.remove(channel.get(0).getString("refrenceID"));
		}
		setItemIDList(exchange, itemIdMap);
		exchange.setProperty("orderWeightMap", weightMap);
		exchange.setProperty("orderSKUMap", skuMap);
	}
}
