package com.sellinall.lazada.services;

import java.io.IOException;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;



@Path("/chat")
@Produces(MediaType.APPLICATION_JSON)
public class ChatService {
	static Logger log = Logger.getLogger(ChatService.class.getName());
	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@GET
	@Path("/conversationList")
	public Response getConversationList(@DefaultValue("") @HeaderParam("accountNumber") String accountNumber,
			@DefaultValue("") @QueryParam("nickNameID") String nickNameID,
			@DefaultValue("20") @QueryParam("pageSize") int pageSize,
			@DefaultValue("0") @QueryParam("nextTimeStamp") Long nextTimeStamp,
			@DefaultValue("") @QueryParam("lastSessionID") String lastSessionID) throws Exception {
		JSONObject json = new JSONObject();
		if (accountNumber.isEmpty() || nickNameID.isEmpty()) {
			json.put("status", "failure");
			json.put("errorMessage", "nicknameID or account number missing");
			return Response.status(Response.Status.BAD_REQUEST).entity(json).build();
		}
		JSONObject request = new JSONObject();
		request.put("accountNumber", accountNumber);
		request.put("nickNameID", nickNameID);
		request.put("pageSize", pageSize);
		request.put("nextTimeStamp", nextTimeStamp);
		request.put("lastSessionID", lastSessionID);
		json = template.requestBody("direct:conversationList", request, JSONObject.class);
		return Response.ok(json, MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/conversation/messages")
	public Response getMessages(@DefaultValue("") @HeaderParam("accountNumber") String accountNumber,
			@DefaultValue("") @QueryParam("nickNameID") String nickNameID,
			@DefaultValue("10") @QueryParam("pageSize") int pageSize,
			@DefaultValue("0") @QueryParam("nextTimeStamp") Long nextTimeStamp,
			@DefaultValue("") @QueryParam("conversationID") String conversationID,
			@DefaultValue("") @QueryParam("lastMessageID") String lastMessageID) throws JSONException {
		JSONObject json = new JSONObject();
		if (accountNumber.isEmpty() || nickNameID.isEmpty() || conversationID.isEmpty()) {
			json.put("status", "failure");
			json.put("errorMessage", "nicknameID or account number missing");
			return Response.status(Response.Status.BAD_REQUEST).entity(json).build();
		}
		JSONObject request = new JSONObject();
		request.put("accountNumber", accountNumber);
		request.put("nickNameID", nickNameID);
		request.put("pageSize", pageSize);
		request.put("nextTimeStamp", nextTimeStamp);
		request.put("conversationID", conversationID);
		request.put("lastMessageID", lastMessageID);
		json = template.requestBody("direct:conversationMessage", request, JSONObject.class);
		return Response.ok(json, MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/conversation/detail")
	public JSONObject getConversationDetails(@HeaderParam("accountNumber") String accountNumber,
			@QueryParam("nickNameID") String nickNameID, @QueryParam("conversationID") String conversationID)
			throws JSONException {
		JSONObject request = new JSONObject();
		request.put("accountNumber", accountNumber);
		request.put("nickNameID", nickNameID);
		request.put("conversationID", conversationID);
		
		return template.requestBody("direct:getConversationDetails", request, JSONObject.class);
	}

	@PUT
	@Path("/conversation")
	public Response manageConversation(@HeaderParam("accountNumber") String accountNumber, JSONObject payload)
			throws JSONException, IOException {
		payload.put("accountNumber", accountNumber);
		String conversationID = "";
		String lastReadMessageID = "";
		if (payload.has("conversationID") && payload.has("lastReadMessageId")) {
			conversationID = payload.getString("conversationID");
			lastReadMessageID = payload.getString("lastReadMessageId");
		}
		JSONObject json = new JSONObject();
		if (conversationID.isEmpty() || lastReadMessageID.isEmpty()) {
			json.put("status", "failure");
			json.put("errorMessage", "conversationID or lastReadMessageID missing");
			return Response.status(Response.Status.BAD_REQUEST).entity(json).build();
		}
		json = template.requestBody("direct:updateSessionAsRead", payload, JSONObject.class);
		return Response.ok(json, MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Path("/message")
	public Response getMessages(@HeaderParam("accountNumber") String accountNumber, JSONObject payload)
			throws JSONException {
		payload.put("accountNumber", accountNumber);
		String nickNameID = "";
		if (payload.has("nickNameID")) {
			nickNameID = payload.getString("nickNameID");
		}
		JSONObject json = new JSONObject();
		if (accountNumber.isEmpty() || nickNameID.isEmpty()) {
			json.put("status", "failure");
			json.put("errorMessage", "nicknameID or account number missing");
			return Response.status(Response.Status.BAD_REQUEST).entity(json).build();
		}
		json = template.requestBody("direct:sendMessage", payload, JSONObject.class);
		return Response.ok(json, MediaType.APPLICATION_JSON).build();
	}

	@DELETE
	@Path("/message")
	public Response recallMessage(@HeaderParam("accountNumber") String accountNumber, JSONObject payload)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		json = template.requestBody("direct:recallMessage", payload, JSONObject.class);
		return Response.ok(json, MediaType.APPLICATION_JSON).build();
	}

}
