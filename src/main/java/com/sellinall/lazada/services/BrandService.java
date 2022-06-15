package com.sellinall.lazada.services;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ProducerTemplate;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/*
 * class is used to get brand details list from specific channels.
 */
@Produces(MediaType.APPLICATION_JSON)
@Path("/brand")
public class BrandService {

	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	/*
	 * this method is used to get brand details from specific channels. account
	 * number and nicknameId is binded to request object and is sent to camel
	 * routes.
	 */
	@GET
	public Response getBrandList(@HeaderParam("accountNumber") String accountNumber,
			@QueryParam("nickNameID") String nickNameID, @QueryParam("offset") String offset,
			@QueryParam("pageLimit") String pageLimit) throws JSONException {
		JSONObject request = new JSONObject();
		request.put("accountNumber", accountNumber);
		request.put("nickNameID", nickNameID);
		request.put("offset", offset);
		request.put("pageLimit", pageLimit);
		JSONObject response = new JSONObject();
		try {
			response = template.requestBody("direct:getBrand", request, JSONObject.class);
			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}
}
