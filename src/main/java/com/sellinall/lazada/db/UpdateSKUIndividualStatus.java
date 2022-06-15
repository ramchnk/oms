/**
 * 
 */
package com.sellinall.lazada.db;

import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.codehaus.jettison.json.JSONException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAInventoryUpdateStatus;

/**
 * @author vikraman
 * 
 */
public class UpdateSKUIndividualStatus implements Processor {
	public void process(Exchange exchange) throws Exception {
		Object[] outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private Object[] createBody(Exchange exchange) throws JSONException {
		BasicDBObject inventory = exchange.getIn().getBody(BasicDBObject.class);
		String channelName = exchange.getProperty("channelName", String.class);
		BasicDBObject channel = (BasicDBObject) inventory.get(channelName);
		String requestType = exchange.getProperty("requestType", String.class);
		DBObject dbquery = new BasicDBObject();
		dbquery.put("SKU", inventory.getString("SKU"));
		dbquery.put(channelName + ".nickNameID", channel.getString("nickNameID"));
		BasicDBObject update = new BasicDBObject();
		BasicDBObject unSetObject = new BasicDBObject();
		String failureReason = "";
		if(exchange.getProperties().containsKey("failureReason")) {
			failureReason = exchange.getProperty("failureReason", String.class);
		}
		boolean isEditSheet = exchange.getProperty("sheetType") != null && exchange.getProperty("sheetType", String.class).equals("EDIT");
		if(requestType.equals("batchVerifyAddItem")) {
			if (!failureReason.isEmpty()) {
				update.append(channelName + ".$.failureReason", failureReason);
			}
			if (!isEditSheet) {
				if(failureReason.isEmpty()) {
					update.append(channelName+".$.status", SIAInventoryStatus.VALIDATED.toString());
				} else {
					update.append(channelName + ".$.status", SIAInventoryStatus.FAILED.toString());
				}
			}
		} else {
			boolean isPostingSuccess = false;
			if (exchange.getProperties().containsKey("isPostingSuccess")) {
				isPostingSuccess = exchange.getProperty("isPostingSuccess", Boolean.class);
			}
			if(isPostingSuccess){
				if (requestType.equals("batchAddItem") || requestType.equals("addItem")) {
					update.append(channelName + ".$.status", SIAInventoryStatus.PENDING.toString());
				} else if (requestType.equals("addVariant")) {
					update.append(channelName + ".$.status", SIAInventoryStatus.ACTIVE.toString());
				} else {
					update.append(channelName + ".$.updateStatus", SIAInventoryUpdateStatus.COMPLETED.toString());
				}
				String referenceKey = "";
				if (inventory.containsField("customSKU")) {
					referenceKey = inventory.getString("customSKU");
					update.append(channelName + ".$.refrenceID", inventory.getString("customSKU"));
				} else {
					referenceKey = inventory.getString("SKU");
					update.append(channelName + ".$.refrenceID", inventory.getString("SKU"));
				}
				if (exchange.getProperties().containsKey("customSKUAndSkuIdMap")) {
					Map<String, String> customSKUAndSkuIdMap = exchange.getProperty("customSKUAndSkuIdMap", Map.class);
					if (customSKUAndSkuIdMap.get(referenceKey) != null) {
						update.append(channelName + ".$.skuID", customSKUAndSkuIdMap.get(referenceKey));
					}
				}
				if (exchange.getProperties().containsKey("itemID")) {
					update.append(channelName + ".$.itemID", exchange.getProperty("itemID", String.class));
				}
				if (exchange.getProperties().containsKey("isPromotionEnabled")
						&& exchange.getProperty("isPromotionEnabled", Boolean.class)) {
					update.append(channelName + ".$.isPromotionEnabled", true);
				} else if (exchange.getProperties().containsKey("isPromotionRemoved")
						&& exchange.getProperty("isPromotionRemoved", Boolean.class)) {
					unSetObject.append(channelName + ".$.isPromotionEnabled", 1);
				}
			} else {
				if (exchange.getProperty("requestType", String.class).equals("batchEditItem")) {
					update.append(channelName + ".$.updateStatus", SIAInventoryUpdateStatus.FAILED.toString())
							.append(channelName + ".$.failureReason", failureReason);
				} else {
					update.append(channelName + ".$.status", SIAInventoryStatus.FAILED.toString())
							.append(channelName + ".$.failureReason", failureReason);
				}
			}
		}
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
		DBObject updateObject = new BasicDBObject("$set", update);
		if (!unSetObject.isEmpty()) {
			updateObject.put("$unset", unSetObject);
		}
		return new Object[] { dbquery, updateObject };
	}
}