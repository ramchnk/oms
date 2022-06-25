/**
 * 
 */
package com.sellinall.lazada.init;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Ram
 * 
 */
public class InitializeGetDocument implements Processor {
	static Logger log = Logger.getLogger(InitializeGetDocument.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		log.debug("InBody: " + inBody);
		exchange.getOut().setBody(inBody);
		exchange.setProperty("documentType", inBody.get("documentType"));
		exchange.setProperty("orderIDs", inBody.get("orderIDs"));
		if (inBody.has("orderIDs")) {
			JSONArray orderItemIDs = inBody.getJSONArray("orderIDs");
			List<String> orderItemIDsList = new ArrayList<String>();
			for (int i = 0; i < orderItemIDs.length(); i++) {
				orderItemIDsList.add(orderItemIDs.getString(i));
			}
			exchange.setProperty("orderItemIDs", orderItemIDsList);
		}
		exchange.setProperty("requestType", "getDocuments");
	}
}