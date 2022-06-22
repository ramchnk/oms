package com.sellinall.lazada.services;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;

@Path("/settings")
@Produces(MediaType.APPLICATION_JSON)
public class AccountService {
	static Logger log = Logger.getLogger(AccountService.class.getName());
	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@GET
	@Path("/initiate")
	public JSONObject startAuth() throws Exception {
		JSONObject request = new JSONObject();

		JSONObject response = template.requestBody("direct:startAuth", request, JSONObject.class);
		response.put("response", "success");
		return response;
	}

	@GET
	@Path("/complete")
	public Object createLinkedAccount(@QueryParam("code") String authToken)
			throws JSONException, UnsupportedEncodingException {
		return processResult(authToken, "", false);
	}

	public Object processResult(String authToken, String signUpChannel, Boolean isAddedByPartner)
			throws JSONException, UnsupportedEncodingException {
		String paramStatus = "FAILURE";
		String responseMessage = "";
		if (authToken != null) {
			JSONObject request = new JSONObject();
			request.put("authToken", authToken);
			template.requestBody("direct:finishAuth", request, JSONObject.class);
			paramStatus = "SUCCESS";
		}
		String settingCompleteUrl = "";
		if (isAddedByPartner != null && isAddedByPartner && signUpChannel != null
				&& Config.getConfig().getPartnerSignupChannels().contains(signUpChannel)) {
			settingCompleteUrl = Config.getConfig().getPartnerSignUpCompletedUrl();
		} else {
			settingCompleteUrl = Config.getConfig().getSettingsCompletedURL();
		}
		return "<script>" + "window.onload = function()" + "{" + "window.opener.location.href='" + settingCompleteUrl
				+ "?Perm=" + paramStatus + "&reason=" + URLEncoder.encode(responseMessage, "UTF-8")
				+ "';window.close();" + "};window.onunload = function(){ window.location.reload(true);};"
				+ "</script>;";
	}
}
