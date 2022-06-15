package com.sellinall.lazada.services;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Path("/policies")
@Produces(MediaType.APPLICATION_JSON)
public class PolicyServices {
	static Logger log = Logger.getLogger(AccountService.class.getName());
	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@PUT
	public JSONObject syncPolicies(@HeaderParam("accountNumber") String accountNumber, JSONObject request)
			throws JSONException {
		log.debug("Inside Sync Policy service" + accountNumber);
		request.put("accountNumber", accountNumber);
		request.put("isUpdatePolicies", false);
		if (request.has("syncPolicy") && request.getBoolean("syncPolicy")) {
			return template.requestBody("direct:syncPolicies", request, JSONObject.class);
		} else if (request.has("syncDeliveryOptions") && request.getBoolean("syncDeliveryOptions")) {
			return template.requestBody("direct:syncDeliveryOptions", request, JSONObject.class);
		} else {
			request.put("isUpdatePolicies", true);
			return template.requestBody("direct:updatePolicies", request, JSONObject.class);
		}
	}

}
