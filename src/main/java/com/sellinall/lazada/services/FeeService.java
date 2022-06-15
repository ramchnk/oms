package com.sellinall.lazada.services;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;


@Path("/fee")
@Produces(MediaType.APPLICATION_JSON)
public class FeeService {

	@GET
	public JSONObject getCommissionFee(@HeaderParam("accountNumber") String accountNumber,
			@QueryParam("nickNameID") String nickNameID) throws JSONException {
		JSONObject response = new JSONObject();
		response.put("commissionFeePercent", Config.getConfig().getLazadaCommissionFeePercent());
		return response;
	}
}
