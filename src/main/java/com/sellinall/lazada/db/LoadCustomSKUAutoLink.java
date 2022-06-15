package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.util.enums.SIAInventoryStatus;

public class LoadCustomSKUAutoLink implements Processor {

	static Logger log = Logger.getLogger(UserDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject itemFromSite = exchange.getProperty("item", JSONObject.class);
		JSONArray variants = new JSONArray();
		// The JSON Array will contain all variant details
		if(itemFromSite.has("skus")) {
			variants = (JSONArray) itemFromSite.get("skus");
		} else {
			variants = (JSONArray) itemFromSite.get("Skus");
		}
		// Now handled only without variant that means first array element of
		// the object
		JSONObject variantsDetails = (JSONObject) variants.get(0);
		BasicDBObject baseQuery = new BasicDBObject("accountNumber", exchange.getProperty("accountNumber", String.class));
		List<SIAInventoryStatus> exclusions = new ArrayList<SIAInventoryStatus>();
		exclusions.add(SIAInventoryStatus.REMOVED);
		List<String> statusValues = getStatusValues(exclusions);
		baseQuery.put("status", new BasicDBObject("$in", statusValues));
		String customSKU = variantsDetails.getString("SellerSku");
		baseQuery.put("customSKU", customSKU);
		
		log.debug("orQuery:" + baseQuery);

		BasicDBObject fieldsFilter = new BasicDBObject("lazada", 1);
		fieldsFilter.put("SKU", 1);
		fieldsFilter.put("customSKU", 1);
		fieldsFilter.put("itemTitle", 1);
		exchange.getOut().setHeader(MongoDbConstants.FIELDS_FILTER, fieldsFilter);
		exchange.getOut().setBody(baseQuery);

	}

	private static List<String> getStatusValues(List<SIAInventoryStatus> exclusions) {
		SIAInventoryStatus[] a = SIAInventoryStatus.values();
		List<String> statusValues = new ArrayList<String>();
		for (int i = 0; i < a.length; i++) {
			if (!exclusions.contains(a[i])) {
				statusValues.add(a[i].toString());
			}
		}
		return statusValues;
	}

}
