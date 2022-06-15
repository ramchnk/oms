package com.sellinall.lazada.services;

import java.io.UnsupportedEncodingException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.bl.DataMoat;

@Path("/dataMoat")
@Produces(MediaType.APPLICATION_JSON)
public class DataMoatService {

	static Logger log = Logger.getLogger(DataMoatService.class.getName());

	@POST
	@Path("/login")
	public static JSONObject callLoginApi(@DefaultValue("NA") @HeaderParam("accountNumber") String accountNumber,
			JSONObject request) throws JSONException, UnsupportedEncodingException {
		request.put("accountNumber", accountNumber);
		return DataMoat.callDataMoatApi(request, "/datamoat/login", "POST");
	}

	@GET
	@Path("/computeRisk")
	public static JSONObject callComputeRiskApi(@HeaderParam("accountNumber") String accountNumber,
			@QueryParam("ati") String ati, @QueryParam("userIP") String userIP, @QueryParam("userID") String userID,
			@QueryParam("time") String time,@QueryParam("appType") String appType) throws JSONException, UnsupportedEncodingException {
		JSONObject request = new JSONObject();
		request.put("ati", ati);
		request.put("userIP", userIP);
		request.put("userID", userID);
		request.put("time", time);
		/*when login success, then only we call this Api*/
		request.put("loginResult", "success");
		request.put("accountNumber", accountNumber);
		request.put("appType", appType);

		return DataMoat.callDataMoatApi(request, "/datamoat/compute_risk", "GET");
	}

}
