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
@Path("/channelData")
public class ChannelDataService {
	static Logger log = Logger.getLogger(ChannelDataService.class.getName());
	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@GET
	public JSONObject getChannelData(@HeaderParam("accountNumber") String accountNumber,
			@QueryParam("nickNameID") String nickNameID, @QueryParam("dataType") String dataType) throws JSONException {
		JSONObject request = new JSONObject();
		request.put("accountNumber", accountNumber);
		request.put("nickNameID", nickNameID);
		request.put("dataType", dataType);

		JSONObject response = new JSONObject();
		if (dataType.equals("freeShippingRegions")) {
			response = template.requestBody("direct:syncDeliveryRegions", request, JSONObject.class);
		}
		return response;
	}
}
