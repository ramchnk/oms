/**
 * 
 */
package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.sellinall.util.enums.SIAInventoryStatus;

/**
 * @author vikraman
 * 
 */
public class UpdateSKUImageUploadStatus implements Processor {
	static Logger log = Logger.getLogger(UpdateSKUImageUploadStatus.class.getName());
	public void process(Exchange exchange) throws Exception {

		String inBody = exchange.getIn().getBody(String.class);
		log.debug("UpdateSKUDBQuery Received sku: " + inBody);
		Object[] outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private Object[] createBody(Exchange exchange) throws JSONException {
		JSONObject inventory = exchange.getProperty("inventory", JSONObject.class);
		String SKU = inventory.getString("SKU");
		String channelName = exchange.getProperty("channelName", String.class);
		JSONObject channel = inventory.getJSONObject(channelName);
		DBObject filterField1 = new BasicDBObject("SKU", SKU);
		DBObject filterField2 = new BasicDBObject(channelName + ".nickNameID", channel.getString("nickNameID"));
		BasicDBList and = new BasicDBList();
		and.add(filterField1);
		and.add(filterField2);
		DBObject filterField = new BasicDBObject("$and", and);

		BasicDBObject update = new BasicDBObject();
		if (exchange.getProperties().containsKey("failureReason")
				&& !exchange.getProperty("failureReason", String.class).isEmpty()) {
			//Api call immediate failure response then flow will come here 
			String failureReason = exchange.getProperty("failureReason", String.class);
			update.append(channelName+".$.status", "F").append(channelName+".$.failureReason", failureReason);	
		}
		else{
			//Status 'W' for lazada rows will display Under Review 
			update.append(channelName+".$.status", SIAInventoryStatus.PENDING.toString());
			update.append(channelName+".$.refrenceID", SKU);
			update.append(channelName+".$.imageStatus", exchange.getIn().getHeader("imageStatus"));
		}
		DBObject updateObject = new BasicDBObject("$set", update);
		log.debug("Image Upload upload Status Query = "+filterField.toString());
		log.debug("Image Upload upload Status data = "+updateObject.toString());
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
		return new Object[] { filterField, updateObject };
	}
}