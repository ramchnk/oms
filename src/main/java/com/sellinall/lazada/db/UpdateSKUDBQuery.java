/**
 * 
 */
package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAInventoryUpdateStatus;

/**
 * @author vikraman
 *
 */
public class UpdateSKUDBQuery implements Processor {
	static Logger log = Logger.getLogger(UpdateSKUDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		Object[] outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private Object[] createBody(Exchange exchange) throws JSONException {
		BasicDBObject inventory = new BasicDBObject();
		if (exchange.getProperty("requestType", String.class).contentEquals("batchEditItem")) {
			inventory = exchange.getIn().getBody(BasicDBObject.class);
		} else {
			inventory = (BasicDBObject) exchange.getProperty("inventory");
		}
		String requestType = (exchange.getProperties().containsKey("requestType")) ?
				exchange.getProperty("requestType", String.class) : "";
		String SKU;
		String nickNameID;
		String channelName;
		if (requestType.equals("quantityChange")) {
			SKU = exchange.getProperty("SKU", String.class);
			nickNameID = exchange.getProperty("nickNameID", String.class);
			channelName = nickNameID.split("-")[0];
		} else {
			SKU = inventory.getString("SKU");
			channelName = exchange.getProperty("channelName", String.class);
			BasicDBObject channel = (BasicDBObject) inventory.get(channelName);
			nickNameID = channel.getString("nickNameID");
		}
		DBObject filterField1 = new BasicDBObject("SKU", SKU);
		DBObject filterField2 = new BasicDBObject(channelName+".nickNameID", nickNameID);
		BasicDBList and = new BasicDBList();
		and.add(filterField1);
		and.add(filterField2);
		DBObject filterField = new BasicDBObject("$and", and);

		BasicDBObject setObject = new BasicDBObject();
		BasicDBObject unSetObject = new BasicDBObject();
		if (exchange.getProperties().containsKey("updateFailureReason")) {
			setObject.put(channelName + ".$.updateStatus", SIAInventoryUpdateStatus.FAILED.toString());
			setObject.put(channelName + ".$.updateFailureReason",
					exchange.getProperty("updateFailureReason", String.class));
			setObject.put(channelName + ".$.failureReason", "");
		} else if (exchange.getProperties().containsKey("failureReason")
				&& !exchange.getProperty("failureReason", String.class).isEmpty()) {
			String failureReason = exchange.getProperty("failureReason", String.class);
			setObject.put(channelName + ".$.failureReason", failureReason);
			setObject.put(channelName + ".$.updateStatus", SIAInventoryUpdateStatus.FAILED.toString());
			setObject.put(channelName + ".$.updateFailureReason", "");
		} else {
			setObject.put(channelName + ".$.updateStatus", SIAInventoryUpdateStatus.COMPLETED.toString());
			setObject.put(channelName + ".$.updateFailureReason", "");
			setObject.put(channelName + ".$.failureReason", "");
			if (exchange.getProperties().containsKey("isPromotionEnabled")
					&& exchange.getProperty("isPromotionEnabled", Boolean.class)) {
				setObject.put(channelName + ".$.isPromotionEnabled", true);
			} else if (exchange.getProperties().containsKey("isPromotionRemoved")
					&& exchange.getProperty("isPromotionRemoved", Boolean.class)) {
				unSetObject.put(channelName + ".$.isPromotionEnabled", 1);
			}
			if (requestType.equals("quantityChange")) {
				boolean isEligibleToUpdateStatus = false;
				if (exchange.getProperties().containsKey("isUpdateStockViaProductUpdateApi")
						&& exchange.getProperty("isUpdateStockViaProductUpdateApi", Boolean.class)) {
					// Note: update stock via update product api only we are updating status as
					// well, that time only we need to update status on our side
					isEligibleToUpdateStatus = true;
				}
				if (isEligibleToUpdateStatus && exchange.getProperty("statusToUpdate") != null
						&& (exchange.getProperty("eligibleToUpdateAutoStatus", Boolean.class) == true
								|| exchange.getProperty("isAutoStatusUpdate", Boolean.class) == true)) {
					exchange.setProperty("isEligibleToUpdatePM", true);
					JSONArray customSKUList = new JSONArray();
					List<String> customIDList = new ArrayList<String>();
					if(exchange.getProperties().containsKey("customSKUList")) {
						customIDList = exchange.getProperty("customSKUList", List.class);
						customSKUList = new JSONArray(customIDList.toString());
					}
					customSKUList.put(exchange.getProperty("refrenceID"));
					if (exchange.getProperty("statusToUpdate", String.class).equals("inactive")) {
						exchange.setProperty("customSKUListToPullProductMaster", customSKUList);
						setObject.put(channelName + ".$.status", SIAInventoryStatus.INACTIVE.toString());
					} else {
						exchange.setProperty("customSKUListToPushProductMaster", customSKUList);
						setObject.put(channelName + ".$.status", SIAInventoryStatus.ACTIVE.toString());
					}
				}
				setObject.put(channelName + ".$.noOfItem", exchange.getProperty("quantity", Integer.class));
			}
		}
		setObject.put(channelName + ".$.timeLastUpdated", System.currentTimeMillis() / 1000L);

		if (requestType.equals("quantityChange") && exchange.getProperties().containsKey("requestQuantities")) {
			updateQuantities(exchange, SKU, setObject);
		}

		DBObject updateObject = new BasicDBObject("$set", setObject);
		if (!unSetObject.isEmpty()) {
			updateObject.put("$unset", unSetObject);
		}
		log.debug(updateObject);
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
		return new Object[] { filterField, updateObject };
	}

	private void updateQuantities(Exchange exchange, String SKU, BasicDBObject setObject) throws JSONException {
		List<BasicDBObject> quantityArray = new ArrayList<BasicDBObject>();
		DBObject inventoryObj = LazadaUtil.getSKUDetails(SKU);
		Map<String, Integer> warehouseIDMap = new HashMap<>();

		if (inventoryObj.containsKey("quantities")) {
			List<BasicDBObject> quantitiesArrayData = (List<BasicDBObject>) inventoryObj.get("quantities");
			for (int j = 0; j < quantitiesArrayData.size(); j++) {
				BasicDBObject invObject = quantitiesArrayData.get(j);
				warehouseIDMap.put(invObject.getString("warehouseID"), j);
			}
		}

		int index = warehouseIDMap.size();
		Map<String, Integer> bufferQuantityMap = getBufferQuantity(exchange);

		JSONArray quantities = exchange.getProperty("requestQuantities", JSONArray.class);
		for (int i = 0; i < quantities.length(); i++) {
			JSONObject quantityObject = (JSONObject) quantities.get(i);
			String warehouseID = quantityObject.getString("warehouseID");
			int netQuantity = quantityObject.getInt("netQuantity");
			int bufferQuantity = 0;

			if (bufferQuantityMap.containsKey(warehouseID)) {
				bufferQuantity = bufferQuantityMap.get(warehouseID);
			}

			if (!inventoryObj.containsKey("quantities")) {
				BasicDBObject quanObj = new BasicDBObject();
				quanObj.put("warehouseID", warehouseID);
				quanObj.put("quantity", netQuantity - bufferQuantity);
				quanObj.put("timeLastUpdated", System.currentTimeMillis()/1000L);
				quantityArray.add(quanObj);
			} else {
				if (warehouseIDMap.containsKey(warehouseID)) {
					setObject.put("quantities." + warehouseIDMap.get(warehouseID) + ".quantity",
							netQuantity - bufferQuantity);
					setObject.put("quantities." + warehouseIDMap.get(warehouseID) + ".timeLastUpdated", System.currentTimeMillis()/1000L);
				} else {
					setObject.put("quantities." + index + ".warehouseID", warehouseID);
					setObject.put("quantities." + index + ".quantity", netQuantity - bufferQuantity);
					setObject.put("quantities." + index + ".timeLastUpdated", System.currentTimeMillis()/1000L);
					index++;
				}
			}
		}

		if (!inventoryObj.containsKey("quantities")) {
			setObject.put("quantities", quantityArray);
		}
	}

	private Map getBufferQuantity(Exchange exchange) throws JSONException {
		Map<String, Integer> bufferQuantityMap = new HashMap<>();
		if (exchange.getProperties().containsKey("listingQuantities")) {
			JSONArray listingQuantities = exchange.getProperty("listingQuantities", JSONArray.class);
			for (int v = 0; v < listingQuantities.length(); v++) {
				JSONObject listingObject = (JSONObject) listingQuantities.get(v);
				if (listingObject.has("bufferQuantity")) {
					bufferQuantityMap.put(listingObject.getString("warehouseID"),
							listingObject.getInt("bufferQuantity"));
				}
			}
		}
		return bufferQuantityMap;
	}
}