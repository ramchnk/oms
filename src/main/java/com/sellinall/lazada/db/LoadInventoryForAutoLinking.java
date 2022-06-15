package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.util.enums.SIAInventoryStatus;


public class LoadInventoryForAutoLinking implements Processor {

	static Logger log = Logger.getLogger(UserDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject itemFromSite = exchange.getProperty("item", JSONObject.class);
		buildHeaderAndQuery(exchange, itemFromSite);
	}

	private void buildHeaderAndQuery(Exchange exchange, JSONObject item) throws JSONException {
		String channelName = exchange.getProperty("channelName", String.class);
		BasicDBObject fieldsFilter = new BasicDBObject(channelName, 1);
		fieldsFilter.put("SKU", 1);
		fieldsFilter.put("customSKU", 1);
		fieldsFilter.put("itemTitle", 1);
		fieldsFilter.put("variants", 1);
		exchange.getOut().setHeader(MongoDbConstants.FIELDS_FILTER, fieldsFilter);
		exchange.getOut().setBody(buildSearchQuery(exchange, item));
	}

	private BasicDBObject buildSearchQuery(Exchange exchange, JSONObject item) throws JSONException {
		BasicDBObject searchQuery = new BasicDBObject();
		List<BasicDBObject> orQuery = new ArrayList<BasicDBObject>();
		JSONArray SKUs = new JSONArray();
		if(item.has("skus")) {
			SKUs = item.getJSONArray("skus");
		} else {
			SKUs = item.getJSONArray("Skus");
		}
		if (SKUs.length() > 1) {
			exchange.setProperty("itemHasVariants", true);
		} else {
			exchange.setProperty("itemHasVariants", false);
		}
		String customSKU = SKUs.getJSONObject(0).getString("SellerSku");
		exchange.setProperty("customSKU", customSKU);
		exchange.setProperty("refrenceID", customSKU);
		String itemID = exchange.getProperty("parentItemID", String.class);
		orQuery.add(loadRefrenceIDQuery(exchange, item, itemID, "itemID"));
		boolean individualSKUPerChannel = exchange.getProperty("individualSKUPerChannel", Boolean.class);
		if(!individualSKUPerChannel) {
			orQuery.add(loadRefrenceIDQuery(exchange, item, customSKU, "refrenceID"));
			orQuery.add(loadCustomSKUQuery(exchange, item, customSKU));
		}
		searchQuery.put("$or", orQuery);
		return searchQuery;
	}

	private BasicDBObject loadRefrenceIDQuery(Exchange exchange, JSONObject item, String value, String type) {
		BasicDBObject elemMatch = new BasicDBObject();
		BasicDBObject query = new BasicDBObject();
		query.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		query.put("status", new BasicDBObject("$ne", SIAInventoryStatus.REMOVED.toString()));
		elemMatch.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		elemMatch.put("variantDetails", new BasicDBObject("$exists", false));
		if (type.equals("itemID")) {
			elemMatch.put("itemID", value);
		} else {
			elemMatch.put("refrenceID", value);
		}
		BasicDBObject searchLazada = new BasicDBObject("$elemMatch", elemMatch);
		query.put("lazada", searchLazada);
		return query;
	}

	private BasicDBObject loadCustomSKUQuery(Exchange exchange, JSONObject item, String customSKU){
		BasicDBObject query = new BasicDBObject();
		query.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		query.put("status", new BasicDBObject("$ne", SIAInventoryStatus.REMOVED.toString()));
		query.put("variantDetails", new BasicDBObject("$exists", false));
		query.put("customSKU", customSKU);
		return query;
	}
}
