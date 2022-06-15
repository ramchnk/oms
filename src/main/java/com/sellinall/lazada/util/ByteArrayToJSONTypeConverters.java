package com.sellinall.lazada.util;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ByteArrayToJSONTypeConverters  implements Processor  {


	public void process(Exchange exchange) throws Exception {
		
		JSONObject json = null;
	
		try {
			json = new JSONObject((String) exchange.getIn().getBody());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		exchange.getOut().setBody(json);
	}

}