package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.util.EncryptionUtil;

public class UpdateAccountDetails implements Processor {
	static Logger log = Logger.getLogger(UpdateAccountDetails.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject request = exchange.getProperty("request", JSONObject.class);
		BasicDBObject update = new BasicDBObject();
		String accountNumber = request.getString("accountNumber");
		DBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", new ObjectId(accountNumber));
		exchange.setProperty("nickNameID", request.getString("nickNameID"));
		searchQuery.put("lazada.nickName.id", request.getString("nickNameID"));
		update = createUpdateData(request, exchange);
		if (update == null) {
			exchange.setProperty("failureReason", "Update failed");
			return;
		}
		DBCollection table = DbUtilities.getDBCollection("accounts");
		table.update(searchQuery, new BasicDBObject("$set", update));
	}

	private static BasicDBObject createUpdateData(JSONObject request, Exchange exchange) {
		BasicDBObject update = new BasicDBObject();
		try {
			BasicDBObject nickName = new BasicDBObject();
			nickName.put("id", exchange.getProperty("nickNameID",String.class));
			if (request.has("storeURL")) {
				update.put("lazada.$.storeURL", request.getString("storeURL"));
			}
			if (request.has("nickName")) {
				nickName.put("value", request.getString("nickName"));
				update.put("lazada.$.nickName", nickName);
			}
			if (request.has("enablePost")) {
				update.put("lazada.$.enablePost", request.getBoolean("enablePost"));
			}
			if (request.has("invoiceTemplate")) {
				update.put("lazada.$.invoiceTemplate", request.getString("invoiceTemplate"));
			}
			if (request.has("shippingLabelTemplate")) {
				update.put("lazada.$.shippingLabelTemplate", request.getString("shippingLabelTemplate"));
			}
			if (request.has("descriptionTemplate")) {
				update.put("lazada.$.descriptionTemplate", request.getString("descriptionTemplate"));
			}
			if(request.has("profile")) {
				update.put("lazada.$.profile", request.getString("profile"));
			}
			if (request.has("shippingCarrier") && request.get("shippingCarrier") instanceof JSONArray
					&& (request.getJSONArray("shippingCarrier")).length() > 0) {
				update.put("lazada.$.shippingCarrier", JSON.parse(request.getJSONArray("shippingCarrier").toString()));
			}
			if (request.has("wms") && request.get("wms") instanceof JSONArray
					&& (request.getJSONArray("wms")).length() > 0) {
				update.put("lazada.$.wms", JSON.parse(request.getJSONArray("wms").toString()));
			}
			//Update ERP
			if (request.has("erp") && request.get("erp") instanceof JSONArray
					&& (request.getJSONArray("erp")).length() > 0) {
				update.put("lazada.$.erp", JSON.parse(request.getJSONArray("erp").toString()));
			}
			if (request.has("deliveryOptionEconomy")) {
				update.put("lazada.$.deliveryOptionEconomy",request.getString("deliveryOptionEconomy"));
			}
			if (request.has("shippingAmount")) {
				update.put("lazada.$.shippingAmount", JSON.parse(request.getJSONObject("shippingAmount").toString()));
			}
			if (request.has("sellerOwnFleet") && exchange.getProperties().containsKey("sofToken")) {
				DBObject sellerOwnFleet = new BasicDBObject();
				String password = request.getJSONObject("sellerOwnFleet").get("password").toString();
				sellerOwnFleet.put("username", request.getJSONObject("sellerOwnFleet").get("username"));
				sellerOwnFleet.put("password", EncryptionUtil.encrypt(password));
				update.put("lazada.$.sellerOwnFleet", sellerOwnFleet);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return update;
	}

}