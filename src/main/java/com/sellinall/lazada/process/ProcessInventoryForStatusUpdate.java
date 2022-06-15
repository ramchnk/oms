package com.sellinall.lazada.process;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.lazada.util.LazadaUtil;

public class ProcessInventoryForStatusUpdate implements Processor {

	public void process(Exchange exchange) throws Exception {
		List<BasicDBObject> inventoryList = exchange.getProperty("inventoryDetails", List.class);
		List<JSONObject> feedInventoryList = new ArrayList<JSONObject>();
		if (!exchange.getProperty("isChildVariantStatusUpdate", boolean.class)) {
			for (int i = 0; i < inventoryList.size(); i++) {
				BasicDBObject inventory = inventoryList.get(i);
				String SKU = inventory.getString("SKU");
				if (!SKU.contains("-")) {
					if (inventoryList.size() == 1) {
						feedInventoryList.add(LazadaUtil.parseToJsonObject(inventory));
					} else {
						List<DBObject> lazadaArray = (List<DBObject>) inventory.get("lazada");
						DBObject lazada = lazadaArray.get(0);
						exchange.setProperty("statusToUpdate", lazada.get("status").toString());
					}
				} else {
					feedInventoryList.add(LazadaUtil.parseToJsonObject(inventory));
				}
			}
		} else {
			BasicDBObject inventory = (BasicDBObject) inventoryList.get(0);
			feedInventoryList.add(LazadaUtil.parseToJsonObject(inventory));
		}
		exchange.getOut().setBody(feedInventoryList);
	}

}
