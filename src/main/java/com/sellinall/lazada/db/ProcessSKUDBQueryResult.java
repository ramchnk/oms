package com.sellinall.lazada.db;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;

import com.mongodb.BasicDBObject;
import com.sellinall.util.enums.SIAInventoryStatus;

public class ProcessSKUDBQueryResult implements Processor {

	static Logger log = Logger.getLogger(ProcessSKUDBQueryResult.class.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		ArrayList<BasicDBObject> inventoryList = (ArrayList<BasicDBObject>) exchange.getIn().getBody();
		String channelName = exchange.getProperty("channelName", String.class);
		BasicDBObject inventory = inventoryList.get(0);
		exchange.setProperty("hasVariants", false);
		String requestType = exchange.getProperty("requestType", String.class);
		if (inventoryList.size() > 1 || requestType.equals("addVariant")) {
			exchange.setProperty("hasVariants", true);
			inventory = getParentInventory(inventoryList);
		}
		inventory.put("channelName", channelName);
		exchange.getOut().setBody(inventory);
		exchange.setProperty("inventory", inventory);
		if (requestType.equals("addItem")) {
			processAddItemRequest(exchange, inventoryList);
		} else if (requestType.equals("addVariant")) {
			processAddVariantRequest(exchange, inventoryList);
		} else {
			// Batch addItem
			processVariantsChannelData(exchange, inventoryList);
		}
		exchange.setProperty("inventoryDetails", inventoryList);
		exchange.setProperty("hasSalePrice", checkIsInventoryHasSalePrice(inventoryList));
	}

	private BasicDBObject getParentInventory(ArrayList<BasicDBObject> inventoryList) {
		for (BasicDBObject inventory : inventoryList) {
			String SKU = inventory.getString("SKU");
			if (!SKU.contains("-")) {
				return inventory;
			}
		}
		// If there is no Parent inventory then will consider first child as
		// parent
		return inventoryList.get(0);
	}

	private BasicDBObject getChildInventory(List<BasicDBObject> inventoryList, List<String> variantsList) {
		BasicDBObject childInventory = null;
		for (Iterator<BasicDBObject> iter = inventoryList.iterator(); iter.hasNext();) {
			BasicDBObject inventory = iter.next();
			String SKU = inventory.getString("SKU");
			if (SKU.contains("-") && !variantsList.contains(SKU)) {
				childInventory = inventory;
				iter.remove();
			}
		}
		return childInventory;
	}

	private void processAddItemRequest(Exchange exchange, ArrayList<BasicDBObject> inventoryList) {
		String SKU = exchange.getProperty("SKU").toString();
		boolean postAsNonVariant = false;
		BasicDBObject postAsNonVariantInventory = new BasicDBObject();
		for (BasicDBObject inventory : inventoryList) {
			if (inventory.getString("SKU").equals(SKU)) {
				ArrayList<BasicDBObject> lazadaList = (ArrayList<BasicDBObject>) inventory.get("lazada");
				BasicDBObject lazada = lazadaList.get(0);
				if (lazada.containsField("postAsNonVariant") && lazada.getBoolean("postAsNonVariant")) {
					inventory.put("lazada", lazada);
					postAsNonVariantInventory = inventory;
					postAsNonVariant = true;
				}
			}
		}
		if (postAsNonVariant) {
			ArrayList<BasicDBObject> postAsNonVariantList = new ArrayList<BasicDBObject>();
			postAsNonVariantList.add(postAsNonVariantInventory);
			exchange.setProperty("postAsNonVariantList", postAsNonVariantList);
			exchange.setProperty("postAsNonVariant", true);
			BasicDBObject inventory = exchange.getProperty("inventory", BasicDBObject.class);
			ArrayList<BasicDBObject> lazadaList = (ArrayList<BasicDBObject>) inventory.get("lazada");
			inventory.put("lazada", lazadaList.get(0));
			return;
		}
		for (BasicDBObject inventory : inventoryList) {
			BasicDBObject lazada = processChannelData((ArrayList<BasicDBObject>) inventory.get("lazada"));
			inventory.put("lazada", lazada);
		}
		exchange.setProperty("inventoryDetails", inventoryList);
	}

	private Boolean checkIsInventoryHasSalePrice(ArrayList<BasicDBObject> inventoryList) {
		for (BasicDBObject inventory : inventoryList) {
			BasicDBObject lazada = (BasicDBObject) inventory.get("lazada");
			if (!lazada.containsField("variants") && (!lazada.containsField("salePrice")
					|| !lazada.containsField("saleStartDate") || !lazada.containsField("saleEndDate"))) {
				return false;
			}
		}
		return true;
	}

	private void processAddVariantRequest(Exchange exchange, ArrayList<BasicDBObject> inventoryList) {
		JSONArray customSKUArray = new JSONArray();
		for (BasicDBObject inventory : inventoryList) {
			BasicDBObject lazada = processChannelData((ArrayList<BasicDBObject>) inventory.get("lazada"));
			inventory.put("lazada", lazada);
			if (inventory.getString("SKU").contains("-") && inventory.containsField("customSKU")) {
				customSKUArray.put(inventory.getString("customSKU"));
			}
		}
		exchange.setProperty("customSKUListToPushProductMaster", customSKUArray);
		String SKU = exchange.getProperty("SKU", String.class);
		List<String> variantsList = exchange.getProperty("variantsList", List.class);
		String associatedSKU;
		BasicDBObject associatedInventory;
		if (variantsList.contains(SKU + "-01")) {
			associatedInventory = getParentInventory(inventoryList);
		} else {
			associatedInventory = getChildInventory(inventoryList, variantsList);
			if(associatedInventory == null) {
				associatedInventory = getParentInventory(inventoryList);
			}
		}
		if (associatedInventory.containsField("customSKU")) {
			associatedSKU = associatedInventory.getString("customSKU");
		} else {
			associatedSKU = associatedInventory.getString("SKU");
		}
		exchange.setProperty("associatedSKU", associatedSKU);
		inventoryList.remove(getParentInventory(inventoryList));
	}

	private void processVariantsChannelData(Exchange exchange, ArrayList<BasicDBObject> inventoryList) {
		// Here we convert arrayList to BasicDbObject
		// query based on nickNameID
		boolean postAsNonVariant = checkIsPostAsNonVariant(inventoryList);
		ArrayList<BasicDBObject> postAsNonVariantList = new ArrayList<BasicDBObject>();
		for (BasicDBObject inventory : inventoryList) {
			BasicDBObject lazada = processChannelData((ArrayList<BasicDBObject>) inventory.get("lazada"));
			inventory.put("lazada", lazada);
			if ((!exchange.getProperties().containsKey("isStatusUpdate")
					|| !exchange.getProperty("isStatusUpdate", Boolean.class))
					&& inventory.getString("SKU").contains("-")) {
				String status = lazada.getString("status");
				if (postAsNonVariant && !status.equals(SIAInventoryStatus.ACTIVE.toString())
						&& !status.equals(SIAInventoryStatus.PENDING.toString())) {
					postAsNonVariantList.add(inventory);
				}

			}
		}
		if (postAsNonVariantList.size() != 0) {
			exchange.setProperty("postAsNonVariantList", postAsNonVariantList);
			exchange.setProperty("postAsNonVariant", true);
		}
		exchange.setProperty("inventoryDetails", inventoryList);
	}

	private BasicDBObject processChannelData(ArrayList<BasicDBObject> channelList) {
		// All ways list have single data only
		// query based on nickNameID
		return (BasicDBObject) channelList.get(0);
	}

	private boolean checkIsPostAsNonVariant(ArrayList<BasicDBObject> inventoryList) {
		for(BasicDBObject inventory : inventoryList){
			ArrayList<BasicDBObject> lazadaList = (ArrayList<BasicDBObject>) inventory.get("lazada");
			BasicDBObject lazada = lazadaList.get(0);
			if(lazada.containsField("postAsNonVariant") && lazada.getBoolean("postAsNonVariant")){
				return true;
			}
		}
		return false;
	}
}
