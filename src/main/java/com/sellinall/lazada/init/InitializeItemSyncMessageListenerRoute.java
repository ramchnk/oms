/**
 * 
 */
package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author vikraman
 *
 */
public class InitializeItemSyncMessageListenerRoute implements Processor {
	static Logger log = Logger.getLogger(InitializeItemSyncMessageListenerRoute.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		log.debug("InitializeItemSyncMessageListnerRoute Received body: " + inBody);
		exchange.getOut().setBody(inBody);
		exchange.setProperty("accountNumber", inBody.get("accountNumber"));
		exchange.setProperty("channelName", inBody.get("channel"));
		exchange.setProperty("allProcessExecutedSuccessfully", true);
	}
}