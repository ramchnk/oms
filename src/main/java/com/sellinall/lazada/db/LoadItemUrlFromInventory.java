/**
 * 
 */
package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;

/**
 * @author Ramchdanran.K
 * 
 */
public class LoadItemUrlFromInventory implements Processor {
	static Logger log = Logger.getLogger(LoadItemUrlFromInventory.class.getName());

	public void process(Exchange exchange) throws Exception {
		DBObject query = createQuery(exchange);
		BasicDBObject fieldsFilter = new BasicDBObject("lazada.$", 1);
		log.debug("Fields : " + fieldsFilter.toString());
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		BasicDBObject inventory = (BasicDBObject) table.findOne(query, fieldsFilter);
		BasicDBList lazadaList = (BasicDBList) inventory.get("lazada");
		BasicDBObject lazada = (BasicDBObject) lazadaList.get(0);
		if (lazada.containsField("itemUrl") && !lazada.getString("itemUrl").equals("")) {
			exchange.setProperty("itemUrl", lazada.getString("itemUrl"));
			exchange.setProperty("isItemActive", true);
			return;
		}
		//Item is not active
		exchange.setProperty("isItemActive", false);
		exchange.getOut().setBody(null);
	}

	private DBObject createQuery(Exchange exchange) throws JSONException {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		String SKU = inBody.getString("SKU");
		String accountNumber = inBody.getString("accountNumber");
		DBObject dbquery = new BasicDBObject();
		dbquery.put("accountNumber", accountNumber);
		dbquery.put("SKU", SKU);
		dbquery.put("lazada.nickNameID", inBody.getString("nickNameID"));
		return dbquery;

	}
}