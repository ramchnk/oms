package com.sellinall.lazada.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;

public class StartAuthRoute implements Processor {
	static Logger log = Logger.getLogger(StartAuthRoute.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		JSONObject response = new JSONObject();
		response.put("response", "success");
		String url = Config.getConfig().getInitiateAuthURL() + "&redirect_uri=" + Config.getConfig().getSelfEndpoint();
		
		url += "/settings/complete/" + inBody.getString("accountNumber");
		
		url += "&client_id=" + Config.getConfig().getLazadaClientID();
		
		response.put("url", url);
		exchange.getOut().setBody(response);
	}
}
