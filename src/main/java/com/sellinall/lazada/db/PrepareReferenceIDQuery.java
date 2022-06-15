package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.WriteConcern;
import com.sellinall.util.enums.SIAInventoryStatus;

public class PrepareReferenceIDQuery implements Processor {
	static Logger log = Logger.getLogger(PrepareReferenceIDQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		String channelName = exchange.getProperty("channelName", String.class);
		Object orderItemResponse = exchange.getIn().getBody(Object.class);
		log.debug("OrderItem = " + orderItemResponse.toString());
		JSONArray orderDetails = new JSONArray();
		ArrayList<String> in = new ArrayList<String>();
		Map<String, String> itemIdMap = new HashMap<String, String>();
		if (orderItemResponse instanceof JSONObject) {
			JSONObject orderItem = new JSONObject();
			orderItem = new JSONObject(orderItemResponse.toString());
			if (orderItem.has("sku")) {
				in.add(orderItem.getString("sku"));
				itemIdMap.put(orderItem.getString("sku"), orderItem.getString("product_id"));
			} else {
				in.add(orderItem.getString("Sku"));
				itemIdMap.put(orderItem.getString("sku"), orderItem.getString("product_id"));
			}
			orderDetails.put(orderItem);
		} else {
			JSONArray list = new JSONArray(orderItemResponse.toString());
			for (int i = 0; i < list.length(); i++) {
				JSONObject orderItem = new JSONObject();
				orderItem = list.getJSONObject(i);
				if (orderItem.has("sku")) {
					in.add(orderItem.getString("sku"));
					itemIdMap.put(orderItem.getString("sku"), orderItem.getString("product_id"));
				} else {
					in.add(orderItem.getString("Sku"));
					itemIdMap.put(orderItem.getString("sku"), orderItem.getString("product_id"));
				}
				orderDetails.put(orderItem);
			}
		}
		exchange.setProperty("refrenceIDList", in);
		exchange.setProperty("orderDetails", orderDetails);
		exchange.setProperty("itemIdMap", itemIdMap);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("refrenceID", new BasicDBObject("$in", in));
		elemMatch.put("nickNameID", exchange.getProperty("nickNameID"));
		elemMatch.put("variants", new BasicDBObject("$exists", false));
		elemMatch.put("status", SIAInventoryStatus.ACTIVE.toString());
		searchQuery.put(channelName, new BasicDBObject("$elemMatch", elemMatch));
		BasicDBObject projection = new BasicDBObject();
		projection.put("SKU", 1);
		projection.put(channelName + ".$", 1);
		log.debug("searchQuery: " + searchQuery);
		exchange.getOut().setBody(searchQuery);
		exchange.getOut().setHeader(MongoDbConstants.FIELDS_FILTER, projection);
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
	}
}