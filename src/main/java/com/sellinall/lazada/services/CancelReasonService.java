package com.sellinall.lazada.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.util.LazadaUtil;

@Path("/cancelReasons")
@Produces(MediaType.APPLICATION_JSON)
public class CancelReasonService {

	@GET
	public static JSONObject getCancelReason() throws JSONException {
		JSONObject response = new JSONObject();
		response.put("cancelReasonList", LazadaUtil.getCancelReasonList());
		return response;
	}
}
