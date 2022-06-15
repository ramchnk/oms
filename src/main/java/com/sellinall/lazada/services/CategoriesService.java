package com.sellinall.lazada.services;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Produces(MediaType.APPLICATION_JSON)
@Path("/categories")
public class CategoriesService {
	static Logger log = Logger.getLogger(CategoriesService.class.getName());
	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@GET
	public String getCategories(@HeaderParam("accountNumber") String accountNumber,
			@QueryParam("nickNameID") String nickNameID) throws JSONException {
		JSONObject request = new JSONObject();
		request.put("accountNumber", accountNumber);
		request.put("nickNameID", nickNameID);
		String response = template.requestBody("direct:getCategory", request, String.class);
		return response;
	}

}
