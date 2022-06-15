/**
 * 
 */
package com.sellinall.lazada.db;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.converters.MongoDbBasicConverters;
import org.apache.log4j.Logger;

import com.mongodb.DBObject;

/**
 * @author Malli
 * 
 */
public class LoadUnlinkedInventoryVariantCount implements Processor {
	static Logger log = Logger.getLogger(LoadUnlinkedInventoryVariantCount.class.getName());
	
	public void process(Exchange exchange) throws Exception {

		DBObject outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private DBObject createBody(Exchange exchange) {
		String SKU = exchange.getProperty("parentSKU", String.class);
		Map<String, Pattern> dbQuery = new HashMap<String, Pattern>();

		Pattern regex = Pattern.compile("^" + SKU + ".*");
		dbQuery.put("SKU", regex);
		log.warn("sku = " + dbQuery.toString());
		return MongoDbBasicConverters.fromMapToDBObject(dbQuery);
	}
}