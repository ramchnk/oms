package com.sellinall.lazada.services;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Path("/documents")
@Produces(MediaType.APPLICATION_JSON)
public class GetDocuments {

	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@Path("/lazada")
	@POST
	public JSONObject getDocument(JSONObject request)
			throws JSONException {
		JSONObject response = template.requestBody("direct:getDocument", request, JSONObject.class);
		if (response != null && response.has("response")) {
			// If auth failure then camel return failure response
			return response;
		}
		// If transaction success then we need to replay success to request
		response = new JSONObject();
		response.put("response", "failure");
		response.put("failureReason", "internal error");
		return response;
	}
}
