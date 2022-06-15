/**
 * 
 */
package com.sellinall.lazada.db;

import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.util.enums.SIAInventoryStatus;

/**
 * @author vikraman
 *
 */
public class CreateSKUDBQuery implements Processor {
	static Logger log = Logger.getLogger(CreateSKUDBQuery.class.getName());
	public void process(Exchange exchange) throws Exception {
		DBObject outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private DBObject createBody(Exchange exchange) throws JSONException {
		String inBody = exchange.getIn().getBody(String.class);
		log.debug("CreateSKUDBQuery Received sku: " + inBody);
		String accountNumber = (String) exchange.getProperty("accountNumber");

		BasicDBObject fieldsFilter = new BasicDBObject("lazada.$", 1);
		fieldsFilter.put("SKU", 1);
		fieldsFilter.put("customSKU", 1);
		fieldsFilter.put("itemTitle", 1);
		fieldsFilter.put("itemDescription", 1);
		fieldsFilter.put("imageURL", 1);
		log.debug("Fields : " + fieldsFilter.toString());
		exchange.getOut().setHeader(MongoDbConstants.FIELDS_FILTER, fieldsFilter);
		BasicDBObject sortBy = new BasicDBObject();
		sortBy.put("SKU", 1);
		exchange.getOut().setHeader(MongoDbConstants.SORT_BY, sortBy);
		DBObject dbquery = new BasicDBObject();
		dbquery.put("accountNumber", accountNumber);
		String SKU = exchange.getProperty("SKU").toString();
		log.debug("sku = " + SKU.toString());
		if (exchange.getProperties().containsKey("isStatusUpdate")
				&& exchange.getProperty("isStatusUpdate", Boolean.class)
				&& exchange.getProperty("isChildVariantStatusUpdate", Boolean.class)) {
			dbquery.put("SKU", SKU);
		} else {
			dbquery.put("SKU", Pattern.compile("^" + SKU.split("-")[0] + ".*"));
		}
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("status", new BasicDBObject("$ne", SIAInventoryStatus.REMOVED.toString()));
		if(exchange.getProperties().containsKey("nickNameID")){
			elemMatch.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		}else if(exchange.getProperties().containsKey("siteNicknames")){
			JSONArray siteNicknames = exchange.getProperty("siteNicknames" ,JSONArray.class);
			String nickNameId = siteNicknames.getString(0);
			elemMatch.put("nickNameID", nickNameId);
			exchange.setProperty("nickNameID", nickNameId);
		}
		dbquery.put("lazada", new BasicDBObject("$elemMatch", elemMatch));
		return dbquery;

	}
}