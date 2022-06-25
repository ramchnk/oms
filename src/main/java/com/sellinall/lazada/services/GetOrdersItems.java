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

@Path("/orderitems")
@Produces(MediaType.APPLICATION_JSON)
public class GetOrdersItems {

	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@GET
	@Path("/lazada")
	public Response getOrders(@QueryParam("orderID") String orderID) throws JSONException {
		JSONObject request = new JSONObject();
		request.put("orderID", orderID);
		JSONObject response = template.requestBody("direct:getOrderDetails", request, JSONObject.class);
		return Response.status(Integer.parseInt(HttpStatus.OK.toString())).entity(response).build();
	}
}
