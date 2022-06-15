package com.sellinall.lazada.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.codehaus.jettison.json.JSONException;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthService {
		@GET
		public int getResponseCode() throws JSONException{
			return HttpStatus.SC_OK;
		}
}
