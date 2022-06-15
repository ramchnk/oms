package com.sellinall.lazada.services;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Path("/description")
@Produces(MediaType.APPLICATION_JSON)
public class DescriptionServices {
	static Logger log = Logger.getLogger(DescriptionServices.class.getName());
	// This is being set by main function
	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@POST
	public JSONObject getDescription(@HeaderParam("accountNumber") String accountNumber, JSONObject request)
			throws JSONException {
		log.debug("Inside Update User" + accountNumber);
		request.put("accountNumber", accountNumber);
		String description = template.requestBody("direct:getItemDescriptionFromChannel", request, String.class);
		JSONObject response = new JSONObject();
		if (description == null) {
			response.put("status", "failure");
			response.put("failureReason", "item is not active");
			return response;
		}
		response.put("status", "success");
		response.put("description", description);
		return response;
	}

}
