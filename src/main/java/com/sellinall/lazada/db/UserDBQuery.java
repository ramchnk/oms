/**
 * 
 */
package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;

/**
 * @author Ram
 *
 */
public class UserDBQuery implements Processor {
	static Logger log = Logger.getLogger(UserDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("from", inBody.getString("from"));
		DBObject outBody = getUserDetails(exchange);
		exchange.getOut().setBody(outBody);
		exchange.setProperty("UserDetails", outBody);
	}

	private DBObject getUserDetails(Exchange exchange) {
		DBCollection table = DbUtilities.getDBCollection("accounts");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("merchantID", "CEO");
		DBObject object = table.findOne(searchQuery);
		return object;
	}
}