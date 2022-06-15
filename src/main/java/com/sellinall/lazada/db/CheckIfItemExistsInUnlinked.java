package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;

import com.mongodb.BasicDBObject;
import com.sellinall.util.enums.SIAUnlinkedInventoryStatus;

public class CheckIfItemExistsInUnlinked implements Processor {

	static Logger log = Logger.getLogger(UserDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		buildHeaderAndQuery(exchange);
	}

	private void buildHeaderAndQuery(Exchange exchange) throws JSONException {
		String channelName = exchange.getProperty("channelName", String.class);
		BasicDBObject fieldsFilter = new BasicDBObject(channelName, 1);
		fieldsFilter.put("SKU", 1);
		fieldsFilter.put("customSKU", 1);
		fieldsFilter.put("itemTitle", 1);
		BasicDBObject searchQuery = new BasicDBObject();
		List<BasicDBObject> orQuery = new ArrayList<BasicDBObject>();
		orQuery.add(buildSearchQuery(exchange, "itemID"));
		orQuery.add(buildSearchQuery(exchange, "refrenceID"));
		searchQuery.put("$or", orQuery);
		exchange.getOut().setHeader(MongoDbConstants.FIELDS_FILTER, fieldsFilter);
		exchange.getOut().setBody(searchQuery);
	}

	private BasicDBObject buildSearchQuery(Exchange exchange, String type) throws JSONException {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		searchQuery.put("lazada.nickNameID", exchange.getProperty("nickNameID", String.class));
		if(type.equals("itemID")) {
			searchQuery.put("lazada.itemID", exchange.getProperty("parentItemID",String.class));
		} else {
			searchQuery.put("lazada.refrenceID", exchange.getProperty("refrenceID",String.class));
		}
		searchQuery.put("variantDetails", new BasicDBObject("$exists", false));
		searchQuery.put("status", new BasicDBObject("$ne", SIAUnlinkedInventoryStatus.REMOVED.toString()));
		return searchQuery;
	}
}
