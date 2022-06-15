package com.sellinall.lazada.db;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.mongodb.util.JSON;

public class ConstructQueryToLoadInventory implements Processor {
	static Logger log = Logger.getLogger(ConstructQueryToLoadInventory.class.getName());

	public void process(Exchange exchange) throws Exception {
		if (exchange.getProperty("isStatusUpdate", boolean.class)) {
			DBObject outBody = createQuery(exchange, exchange.getProperty("isChildVariantStatusUpdate", boolean.class));
			exchange.getOut().setBody(outBody);
			return;
		}
		if (exchange.getProperty("isImagesUpdate", boolean.class)) {
			DBObject outBody = createQueryByskuList(exchange);
			exchange.getOut().setBody(outBody);
			return;
		}
		DBObject outBody = createQuery(exchange, true);
		exchange.getOut().setBody(outBody);

	}

	private DBObject createQueryByskuList(Exchange exchange) {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		DBObject dbquery = new BasicDBObject();
		dbquery.put("accountNumber", accountNumber);
		if (exchange.getProperties().containsKey("isUpdateParentImages")
				&& exchange.getProperty("isUpdateParentImages", boolean.class)
				&& exchange.getProperty("skuList", List.class).size() == 1) {
			BasicDBObject regex = new BasicDBObject();
			regex.put("$regex", "^" + exchange.getProperty("SKU", String.class) + ".*");
			dbquery.put("SKU", regex);
		} else {
			BasicDBObject inQuery = new BasicDBObject("$in", exchange.getProperty("skuList", List.class));
			dbquery.put("SKU", inQuery);
		}
		BasicDBObject fieldsFilter = new BasicDBObject("lazada.$", 1);
		fieldsFilter.put("SKU", 1);
		fieldsFilter.put("customSKU", 1);
		fieldsFilter.put("itemTitle", 1);
		fieldsFilter.put("itemDescription", 1);
		fieldsFilter.put("imageURL", 1);
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("status", new BasicDBObject("$ne", SIAInventoryStatus.REMOVED.toString()));
		if (exchange.getProperties().containsKey("siteNicknames")) {
			List<String> siteNicknameList = (List<String>) JSON.parse(exchange.getProperty("siteNicknames").toString());
			String nickNameId = siteNicknameList.get(0);
			elemMatch.put("nickNameID", nickNameId);
		}
		exchange.getOut().setHeader(MongoDbConstants.FIELDS_FILTER, fieldsFilter);
		BasicDBObject sortBy = new BasicDBObject();
		sortBy.put("SKU", 1);
		exchange.getOut().setHeader(MongoDbConstants.SORT_BY, sortBy);
		dbquery.put("lazada", new BasicDBObject("$elemMatch", elemMatch));
		return dbquery;

	}

	private DBObject createQuery(Exchange exchange, boolean isChildVariantStatusUpdate) {
		String SKU = exchange.getProperty("SKU", String.class);
		String accountNumber = (String) exchange.getProperty("accountNumber");
		boolean isRemoveItem = false;
		if (exchange.getProperties().containsKey("requestType")
				&& exchange.getProperty("requestType", String.class).equals("removeArrayItem")) {
			isRemoveItem = true;
		}
		DBObject dbquery = new BasicDBObject();
		dbquery.put("accountNumber", accountNumber);
		if (isChildVariantStatusUpdate && !isRemoveItem) {
			dbquery.put("SKU", SKU);
		} else {
			dbquery.put("SKU", Pattern.compile("^" + SKU + ".*"));
		}
		if (exchange.getProperties().containsKey("requestType")
				&& exchange.getProperty("requestType", String.class).equals("updateItem")) {
			BasicDBObject elemMatch = new BasicDBObject("status",
					new BasicDBObject("$in",
							Arrays.asList(SIAInventoryStatus.ACTIVE.toString(), SIAInventoryStatus.INACTIVE.toString(),
									SIAInventoryStatus.PENDING.toString(), SIAInventoryStatus.NOT_LISTED.toString())));
			dbquery.put("lazada", new BasicDBObject("$elemMatch", elemMatch));
		}
		DBObject fieldsFilter = new BasicDBObject();
		fieldsFilter.put("SKU", 1);
		fieldsFilter.put("itemDescription", 1);
		fieldsFilter.put("customSKU", 1);
		fieldsFilter.put("variants", 1);
		if (exchange.getProperties().containsKey("nickNameID")) {
			dbquery.put("lazada.nickNameID", exchange.getProperty("nickNameID", String.class));
			fieldsFilter.put("lazada.$", 1);
		} else if (exchange.getProperties().containsKey("siteNicknames")) {
			List<String> siteNicknameList = (List<String>) JSON.parse(exchange.getProperty("siteNicknames").toString());
			String nickNameId = siteNicknameList.get(0);
			dbquery.put("lazada.nickNameID", nickNameId);
			if (exchange.getProperty("isStatusUpdate", boolean.class)) {
				fieldsFilter.put("lazada.$", 1);
			} else {
				fieldsFilter.put("lazada", 1);
			}
			exchange.setProperty("nickNameID", nickNameId);
		} else {
			fieldsFilter.put("lazada", 1);
		}
		exchange.getOut().setHeader(MongoDbConstants.FIELDS_FILTER, fieldsFilter);
		return dbquery;
	}

}
