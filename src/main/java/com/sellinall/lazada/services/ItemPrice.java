package com.sellinall.lazada.services;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Path("inventory/price")
@Produces(MediaType.APPLICATION_JSON)
public class ItemPrice {
	static Logger log = Logger.getLogger(ItemPrice.class.getName());
	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@GET
	public Object getItemPrice(@QueryParam("itemID") String itemID,
			@QueryParam("countryCode") String countryCode) throws JSONException, IOException {
		JSONObject data = new JSONObject();
		data.put("itemID", itemID);
		data.put("countryCode", countryCode);
		return template.requestBody("direct:getItemPrice", data, JSONObject.class);
	}
}
