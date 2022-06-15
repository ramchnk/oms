package com.sellinall.lazada.services;

import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
public class Listing {
	static Logger log = Logger.getLogger(Listing.class.getName());
	// This is being set by main function
	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@POST
	@Path("/{param}")
	public JSONObject post(@HeaderParam("accountNumber") String accountNumber, @PathParam("param") String sku, JSONObject payload) throws Exception {
		JSONObject json = new JSONObject();
		log.debug("sku: " + sku);
		log.debug("payload: " + payload);
		json.put("SKU", sku);
		json.put("accountNumber", accountNumber);
		json.put("siteNicknames", payload.getJSONArray("siteNicknames"));
		json.put("isDeleteOperation", false);
		template.sendBody("direct:start", json);
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("response", "success");
		return jsonResponse;
	}

	@PUT
	@Path("/{param}")
	public JSONObject put(@HeaderParam("accountNumber") String accountNumber, @PathParam("param") String sku, JSONObject payload) throws Exception {
		JSONObject json = new JSONObject();
		log.debug("sku: " + sku);
		log.debug("payload: " + payload);
		json.put("SKU", sku);
		json.put("accountNumber", accountNumber);
		json.put("payload", payload);
		json.put("isDeleteOperation", false);
		template.sendBody("direct:startUpdate", json);
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("response", "success");
		return jsonResponse;
	}
	
	@DELETE
	@Path("/{param}")
	public JSONObject doDelete(@PathParam("param") String sku) throws Exception {
		JSONObject json = new JSONObject();
		log.debug("SKU: " + sku);
		json.put("SKU", sku);
		json.put("isDeleteOperation", true);
		template.sendBody("direct:startDelete", json);
		json.put("response", "Success");
		return json;
	}
}
