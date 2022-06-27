package com.sellinall.lazada.services;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.http.HttpStatus;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON)
public class LoginService {
	@POST
	public Response getResponseCode(JSONObject request) throws JSONException {
		String userName = request.getString("userID");
		String password = request.getString("password");
		if (userName.equals("abc") && password.equals("abc")) {
			return Response.status(Integer.parseInt(HttpStatus.ACCEPTED.toString())).build();
		}
		return Response.status(Integer.parseInt(HttpStatus.UNAUTHORIZED.toString())).build();
	}
}
