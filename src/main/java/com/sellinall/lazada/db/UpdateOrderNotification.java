package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.sellinall.util.DateUtil;

public class UpdateOrderNotification implements Processor {
	static Logger log = Logger.getLogger(UpdateOrderNotification.class.getName());

	public void process(Exchange exchange) throws Exception {
		DBObject outBody = createQuery(exchange);
		exchange.getOut().setBody(outBody);
	}

	private BasicDBObject createQuery(Exchange exchange) throws JSONException {
		BasicDBObject notification = new BasicDBObject();
		BasicDBObject site = new BasicDBObject();
		site.put("name", exchange.getProperty("channelName",String.class));
		site.put("updateOrderFrom", "sellInAll");
		notification.put("site", site);
		notification.put("raw_data", JSON.parse(exchange.getProperty("order").toString()));
		notification.put("time_received", DateUtil.getSIADateFormat());
		exchange.getOut().setHeader("supportedMessage", true);
		return notification;
	}
}