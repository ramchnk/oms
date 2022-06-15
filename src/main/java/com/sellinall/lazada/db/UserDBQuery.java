/**
 * 
 */
package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;

/**
 * @author vikraman
 *
 */
public class UserDBQuery implements Processor {
	static Logger log = Logger.getLogger(UserDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {

		DBObject outBody = getUserDetails(exchange);
		exchange.getOut().setBody(outBody);
		exchange.setProperty("UserDetails", outBody);
	}

	private DBObject getUserDetails(Exchange exchange) {
		try {
			JSONObject inbody = exchange.getIn().getBody(JSONObject.class);
			if (inbody.has("nickNameID")) {
				exchange.setProperty("nickNameID", inbody.getString("nickNameID"));
			}
		} catch (Exception e) {
			log.debug("in body is not a json object");
		}
		String accountNumber = (String) exchange.getProperty("accountNumber");
		log.debug("accountNumber"+accountNumber);
		DBCollection table = DbUtilities.getDBCollection("accounts");
		BasicDBObject searchQuery = new BasicDBObject();
		ObjectId objid = new ObjectId(accountNumber);
		searchQuery.put("_id", objid);
		DBObject object = table.findOne(searchQuery);
		object.put("channelName", "lazada");
		return object;
	}
}