package com.sellinall.lazada.services;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Produces(MediaType.APPLICATION_JSON)
@Path("/category")
public class CategoryAttributeService {
	static Logger log = Logger.getLogger(CategoryAttributeService.class.getName());
	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@GET
	@Path("/{categoryID}/attributes")
	public String getCategoryAttributes(@HeaderParam("accountNumber") String accountNumber,
			@QueryParam("nickNameID") String nickNameID, @PathParam("categoryID") String categoryID)
			throws JSONException {
		JSONObject request = new JSONObject();
		request.put("accountNumber", accountNumber);
		request.put("nickNameID", nickNameID);
		request.put("categoryID", categoryID);
		String response = template.requestBody("direct:getCategoryAttributes", request, String.class);
		return response;
	}

}
