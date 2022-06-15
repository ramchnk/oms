package com.sellinall.lazada.services;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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

	@POST
	@Path("/initiate")
	public JSONObject startAuth(@HeaderParam("accountNumber") String accountNumber,
			JSONObject payload) throws Exception {
		JSONObject request = new JSONObject();
		request.put("accountNumber", accountNumber);
		if(payload.has("nickNameID") && !payload.getString("nickNameID").isEmpty()) {
			request.put("nickNameID", payload.getString("nickNameID"));
		}
		if (payload.has("appType") && !payload.getString("appType").isEmpty()) {
			request.put("appType", payload.getString("appType"));
		}
		if (payload.has("requestFrom")) {
			request.put("requestFrom", payload.getString("requestFrom"));
		}
		if (payload.has("signUpChannel") && !payload.getString("signUpChannel").isEmpty()) {
			request.put("signUpChannel", payload.getString("signUpChannel"));
		}
		JSONObject response = template.requestBody("direct:startAuth", request, JSONObject.class);
		response.put("response", "success");
		return response;
	}

	@GET
	@Path("/complete/{accountNumber}")
	public Object createLinkedAccount(@QueryParam("code") String authToken,
			@PathParam("accountNumber") String accountNumber) throws JSONException, UnsupportedEncodingException {
		return processResult(authToken, accountNumber, "", false);
	}
	@GET
	@Path("/complete/chat/{accountNumber}/{nickNameID}")
	public Object createChatLinkedAccount(@QueryParam("code") String authToken,
			@PathParam("accountNumber") String accountNumber, @PathParam("nickNameID") String nickNameID,
			@QueryParam("requestFrom") @DefaultValue("") String requestFrom)
			throws JSONException, UnsupportedEncodingException {
		if (accountNumber == null) {
			return "<script>" + "window.onload = function()" + "{" + "window.opener.location.href='"
					+ Config.getConfig().getSettingsCompletedURL() +"?code="+ authToken + "';window.close();"
					+ "};window.onunload = function(){ window.location.reload(true);};" + "</script>;";
		}
		JSONObject request = new JSONObject();
		request.put("authToken", authToken);
		request.put("appType", "chat");
		request.put("accountNumber", accountNumber);
		request.put("nickNameID", nickNameID);
		JSONObject response = template.requestBody("direct:finishAuth", request, JSONObject.class);

		String paramStatus = "FAILURE";
		String responseMessage = "";
		if (response.getString("response").equals("failure")) {
			responseMessage = response.getString("responseMessage");
		} else {
			paramStatus = "SUCCESS";
		}
		if (requestFrom.equals("admin")) {
			String merchantID = response.has("merchantID") ? response.getString("merchantID") : "";
			return "<script>" + "window.onload = function()" + "{" + "window.opener.location.href='"
					+ Config.getConfig().getChatReauthCompletedURLAdmin() + "?merchantID=" + merchantID + "&Perm="
					+ paramStatus + "&reason=" + URLEncoder.encode(responseMessage, "UTF-8") + "';window.close();"
					+ "};window.onunload = function(){ window.location.reload(true);};" + "</script>;";
		}
		return "<script>" + "window.onload = function()" + "{" + "window.opener.location.href='"
				+ Config.getConfig().getSettingsCompletedURL() + "?Perm=" + paramStatus
				+ "&isChatAuthentication=true&reason=" + URLEncoder.encode(responseMessage, "UTF-8")
				+ "';window.close();" + "};window.onunload = function(){ window.location.reload(true);};"
				+ "</script>;";
	}

	@GET
	@Path("/complete/{accountNumber}/{isAddedByPartner}/{signUpChannel}")
	public Object createLinkedAccountByPartnerChannel(@QueryParam("code") String authToken,
			@PathParam("accountNumber") String accountNumber, @PathParam("signUpChannel") String signUpChannel,
			@PathParam("isAddedByPartner") Boolean isAddedByPartner)
			throws JSONException, UnsupportedEncodingException {
		return processResult(authToken, accountNumber, signUpChannel, isAddedByPartner);
	}

	public Object processResult(String authToken, String accountNumber, String signUpChannel, Boolean isAddedByPartner)
			throws JSONException, UnsupportedEncodingException {
		String paramStatus = "FAILURE";
		String responseMessage = "";
		if (authToken != null) {
			JSONObject request = new JSONObject();
			request.put("authToken", authToken);
			request.put("accountNumber", accountNumber);
			JSONObject response = new JSONObject();
			response = template.requestBody("direct:finishAuth", request, JSONObject.class);
			if (response.getString("response").equals("failure")) {
				responseMessage = response.getString("responseMessage");
			} else {
				paramStatus = "SUCCESS";
			}
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
	
	@GET
	@Path("/complete/{accountNumber}/{nickNameID}")
	public Object oauthChange(@QueryParam("code") String authToken, @PathParam("accountNumber") String accountNumber,
			@PathParam("nickNameID") String nickNameID) throws JSONException, UnsupportedEncodingException {
		String paramStatus = "FAILURE";
		String responseMessage = "";
		if (authToken != null) {
			JSONObject request = new JSONObject();
			request.put("authToken", authToken);
			request.put("accountNumber", accountNumber);
			request.put("nickNameID", nickNameID);
			JSONObject response = new JSONObject();			
			response = template.requestBody("direct:upgradeAccount", request, JSONObject.class);	
			if (response.getString("response").equals("failure")) {
				responseMessage = response.getString("responseMessage");
			} else {
				paramStatus = "SUCCESS";
			}
		}
		return "<script>" + "window.onload = function()" + "{" + "window.opener.location.href='"
				+ Config.getConfig().getSettingsCompletedURL() + "?Perm=" + paramStatus + "&reason="
				+ URLEncoder.encode(responseMessage, "UTF-8") + "';window.close();"
				+ "};window.onunload = function(){ window.location.reload(true);};" + "</script>;";
	}

	@PUT
	public JSONObject updateAccount(@HeaderParam("accountNumber") String accountNumber, JSONObject request)
			throws JSONException {
		log.debug("Inside Update User" + accountNumber);
		request = request.getJSONObject("data");
		request.put("accountNumber", accountNumber);
		JSONObject response = template.requestBody("direct:updateAccount", request, JSONObject.class);
		return response;
	}
}
