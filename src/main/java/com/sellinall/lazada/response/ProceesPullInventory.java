package com.sellinall.lazada.response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class ProceesPullInventory implements Processor {
	static Logger log = Logger.getLogger(ProceesPullInventory.class.getName());

	public void process(Exchange exchange) throws Exception {
		
		exchange.getOut().setBody(exchange.getIn().getBody());
		JSONObject magentoItem = (JSONObject)exchange.getIn().getBody();
		if(magentoItem.getString("type_id").equals("simple")){
			//Now Handled only type = simple
			exchange.setProperty("processItem", true);
		}else{
			int noOfItemSkipped = (Integer) exchange.getProperty("noOfItemSkipped");
			exchange.setProperty("noOfItemSkipped", noOfItemSkipped+1);
			
			exchange.setProperty("processItem", false);
		}
		
	
	}
}