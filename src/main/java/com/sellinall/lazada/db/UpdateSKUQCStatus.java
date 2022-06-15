package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.sellinall.util.enums.SIAInventoryStatus;

public class UpdateSKUQCStatus implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		if (!inBody.has("status") && !inBody.has("failureReason") && !inBody.has("itemURL")) {
			//Item activated but not yet uploaded to channel
			exchange.getOut().setBody(null);
			return;
		}
		Object[] outBody = createBody(exchange, inBody);
		exchange.getOut().setBody(outBody);
	}

	private Object[] createBody(Exchange exchange, JSONObject inBody) throws JSONException {
		Map<String, String> skuAndRefrenceIdMap = (HashMap<String, String>) exchange.getProperty("skuAndRefrenceIdMap");
		String sellerSKU = inBody.getString("sellerSKU");
		String SKU = skuAndRefrenceIdMap.get(sellerSKU);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		List<String> skuList = new ArrayList<String>();
		if (SKU != null) {
			skuList.add(SKU);
			if (SKU.contains("-")) {
				skuList.add(SKU.split("-")[0]);
			}
		}
		searchQuery.put("SKU", new BasicDBObject("$in", skuList));
		searchQuery.put("lazada.nickNameID", exchange.getProperty("nickNameID").toString());

		BasicDBObject setObject = new BasicDBObject();
		if (inBody.has("failureReason") && !inBody.getString("failureReason").isEmpty()) {
			setObject.put("lazada.$.status", SIAInventoryStatus.FAILED.toString());
			setObject.put("lazada.$.failureReason", inBody.getString("failureReason"));
		} else if (inBody.has("status") && inBody.getString("status").equalsIgnoreCase("inactive")) {
			setObject.put("lazada.$.status", SIAInventoryStatus.INACTIVE.toString());
			if (inBody.has("itemURL")) {
				setObject.put("lazada.$.itemUrl", inBody.getString("itemURL"));
			}
			if (inBody.has("shopSKU")) {
				setObject.put("lazada.$.shopSKU", inBody.getString("shopSKU"));
			}
		} else if (inBody.has("status") && inBody.getString("status").equalsIgnoreCase("deleted")) {
			setObject.put("lazada.$.status", SIAInventoryStatus.REMOVED.toString());
		} else {
			setObject.put("lazada.$.status", SIAInventoryStatus.ACTIVE.toString());
			if (inBody.has("itemURL")) {
				setObject.put("lazada.$.itemUrl", inBody.getString("itemURL"));
			}
			if (inBody.has("shopSKU")) {
				setObject.put("lazada.$.shopSKU", inBody.getString("shopSKU"));
			}
		}
		DBObject updateObject = new BasicDBObject("$set", setObject);
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
		//Like will update parent status also (parent and any one child will have same id)
		exchange.getOut().setHeader(MongoDbConstants.MULTIUPDATE, true);
		return new Object[] { searchQuery, updateObject };
	}
}
