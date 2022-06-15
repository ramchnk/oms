/**
 * 
 */
package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author malli
 *
 */
public class InitSKUMap implements Processor {
	static Logger log = Logger.getLogger(InitSKUMap.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject SKUMap = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("SKUMap", SKUMap);
		exchange.setProperty("SKU", SKUMap.getString("SKU"));
	}
}