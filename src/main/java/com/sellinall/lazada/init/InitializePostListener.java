/**
 * 
 */
package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Senthil
 *
 */
public class InitializePostListener implements Processor {
	static Logger log = Logger.getLogger(InitializePostListener.class.getName());
	
	public void process(Exchange exchange) throws Exception {
		exchange.setProperty("StartTime", System.currentTimeMillis());
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("requestType", inBody.get("requestType"));
		if (inBody.has("accountNumber")) {
			exchange.setProperty("accountNumber", inBody.getString("accountNumber"));
		}
		if (inBody.has("action")) {
			exchange.setProperty("action", inBody.get("action"));
		}
		log.info("Message Listener = " + inBody.toString());
		exchange.getOut().setBody(inBody);
	}

}