package com.sellinall.lazada.services;


import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Path("/order")
@Produces(MediaType.APPLICATION_JSON)
public class GetAPIDetailService {

	static Logger log = Logger.getLogger(GetAPIDetailService.class.getName());

	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@GET
	@Path("/{siteNicknameId}")
	public JSONObject getApiDetails(@PathParam("siteNicknameId") String siteNicknameId,
			@QueryParam("accountNumber") String accountNumber, @QueryParam("apiName") String apiName,
			@QueryParam("orderID") String orderID, @QueryParam("itemID") String itemID,
			@QueryParam("sellerSKU") String sellerSKU, @QueryParam("fromDate") String fromDate,
			@QueryParam("toDate") String toDate, @QueryParam("pageNumber") String pageNumber,
			@QueryParam("isStatusMismatch") Boolean isStatusMismatch) throws JSONException {
		log.debug("request received for " + accountNumber);
		JSONObject request = new JSONObject();
		JSONObject response = null;
		try {
			request.put("accountNumber", accountNumber);
			request.put("nickNameID", siteNicknameId);
			if (isStatusMismatch != null) {
				request.put("isStatusMismatch", isStatusMismatch);
			}
			if (pageNumber != null) {
				request.put("pageNumber", pageNumber);
			}
			if (orderID != null) {
				request.put("orderID", orderID);
			} else if (itemID != null) {
				request.put("itemID", itemID);
			} else if (sellerSKU != null) {
				request.put("sellerSKU", sellerSKU);
			}
			if (apiName != null) {
				request.put("apiName", apiName);
				response = template.requestBody("direct:getApiResponseFromLazada", request, JSONObject.class);
			} else if (fromDate != null && toDate != null) {
				request.put("fromDate", fromDate);
				request.put("toDate", toDate);
				response = template.requestBody("direct:getOrdersFromLazada", request, JSONObject.class);
			}
		} catch (Exception e) {
			log.error("Error occurred while getting order Details for accountNumber: " + accountNumber
					+ " and for nickNameId: " + siteNicknameId + " and for orderID: " + orderID
					+ " and the error is - ", e);
			response = new JSONObject();
			response.put("status", "failure");
			response.put("errorMessage", "Unable to get order details, please retry after some time.");
		}
		return response;
	}

	@POST
	@Path("/syncOrders")
	public JSONObject syncMissingOrder(JSONObject request) throws JSONException {
		JSONObject response = new JSONObject();
		try {
			request.put("apiName", "getOrderDetails");
			request.put("isSyncMissingOrder", true);
			template.asyncSendBody("direct:syncMissingOrder", request);
			response.put("status", "success");
			response.put("message", "succefully order synced");
		} catch (Exception e) {
			e.printStackTrace();
			response.put("status", "failure");
			response.put("errorMessage", "Unable to sync order details, please retry after some time.");
		}
		return response;
	}

}
