package com.sellinall.lazada.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ProducerTemplate;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.http.HttpStatus;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
public class GetOrders {

	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@GET
	@Path("/lazada")
	public Response getOrders(@QueryParam("from") String from) throws JSONException {
		JSONObject request = new JSONObject();
		request.put("from", from);
		JSONObject response = template.requestBody("direct:pullOders", request, JSONObject.class);
		return Response.status(Integer.parseInt(HttpStatus.OK.toString())).entity(response).build();
	}
}
