package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.database.DbUtilities;

public class UpdateDeliveryOptions implements Processor {

	public void process(Exchange exchange) throws Exception {
		DBCollection table = DbUtilities.getDBCollection("accounts");
		DBObject update = new BasicDBObject();
		String nickNameId = exchange.getProperty("nickNameID", String.class);
		String channelName = nickNameId.split("-")[0];
		String accountNumber = exchange.getProperty("accountNumber", String.class);

		DBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", new ObjectId(accountNumber));
		searchQuery.put(channelName + ".nickName.id", nickNameId);
		List<DBObject> deliveryOption = exchange.getIn().getBody(ArrayList.class);
		update.put(channelName + ".$.shippingDeliveryOptions", deliveryOption);
		table.update(searchQuery, new BasicDBObject("$set", update));

		JSONObject response = new JSONObject();
		response.put("response", "success");
		exchange.getOut().setBody(response);
	}
}
