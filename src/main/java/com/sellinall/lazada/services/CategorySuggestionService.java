package com.sellinall.lazada.services;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * 
 * @author Raguvaran.S
 *
 */

@Produces(MediaType.APPLICATION_JSON)
@Path("/categorySuggestion")
public class CategorySuggestionService {
	static Logger log = Logger.getLogger(CategorySuggestionService.class.getName());
	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@GET
	public Response getCategorySuggestion(@DefaultValue("") @HeaderParam("accountNumber") String accountNumber,
			@DefaultValue("") @QueryParam("nickNameID") String nickNameID,
			@DefaultValue("") @QueryParam("itemTitle") String itemTitle) throws JSONException {
		JSONObject json = new JSONObject();
		if (accountNumber.isEmpty() || nickNameID.isEmpty() || itemTitle.isEmpty()) {
			json.put("status", "failure");
			json.put("errorMessage", "account Number or nicknameId or itemTitle missing");
			return Response.status(Response.Status.BAD_REQUEST).entity(json).build();
		}
		log.info("Get category suggestion request received for itemTitle " + itemTitle + " and accountnumber "
				+ accountNumber + " and nicknameId " + nickNameID);
		JSONObject request = new JSONObject();
		request.put("accountNumber", accountNumber);
		request.put("nickNameID", nickNameID);
		request.put("itemTitle", itemTitle);
		JSONObject response = new JSONObject();
		try {
			response = template.requestBody("direct:getCategorySuggestion", request, JSONObject.class);
			if (response.has("response") && response.getString("response").equals("success")) {
				return Response.status(Response.Status.OK).entity(response.getString("payload")).build();
			}
			return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
		} catch (Exception e) {
			e.printStackTrace();
			json.put("status", "failure");
			json.put("errorMessage", "Internal Error");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(json).build();
		}
	}

}
