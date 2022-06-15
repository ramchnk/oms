/**
 * 
 */
package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.util.enums.SIAInventoryUpdateStatus;

/**
 * @author Ramchdanran.K
 * 
 */
public class LoadInventoryByStatus implements Processor {
	static Logger log = Logger.getLogger(LoadInventoryByStatus.class.getName());

	public void process(Exchange exchange) throws Exception {
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("nickNameID", nickNameID);
		if(exchange.getIn().getHeaders().containsKey("updateFailureReason")){
			elemMatch.put("updateStatus", SIAInventoryUpdateStatus.FAILED.toString());
			ArrayList<BasicDBObject> orQuery = new ArrayList<BasicDBObject>();
			BasicDBObject failure = new BasicDBObject("updateFailureReason", exchange.getIn().getHeader("updateFailureReason"));
			orQuery.add(failure);
			failure = new BasicDBObject("failureReason", "Product UNEXPECT_EXCEPTION");
			orQuery.add(failure);
			elemMatch.put("$or", orQuery);
		} else {
			String status = exchange.getIn().getHeader("status", String.class);
			elemMatch.put("status", status);
		}
		if (exchange.getIn().getHeaders().containsKey("loadParentInventory")
				&& !exchange.getIn().getHeader("loadParentInventory", Boolean.class)) {
			elemMatch.put("variants", new BasicDBObject("$exists", false));
		}
		BasicDBObject lazada = new BasicDBObject("$elemMatch", elemMatch);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber", String.class));	
		searchQuery.put("lazada", lazada);

		BasicDBObject fieldsFilter = new BasicDBObject("lazada.$", 1);
		fieldsFilter.put("SKU", 1);
		
		DBCollection inventory = DbUtilities.getROInventoryDBCollection("inventory");
		List<DBObject> result = inventory.find(searchQuery, fieldsFilter).toArray();
		exchange.getOut().setBody(result);
	}
}