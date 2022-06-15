package com.sellinall.lazada.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;;

public class ValidateMandatoryFields implements Processor {

	ArrayList<String> LAZADA_MANDATORY_FIELDS = new ArrayList<String>(Arrays.asList("name", "SellerSku",
			"short_description", "description", "warranty_type", "warrantyType", "brand", "model", "quantity", "price",
			"special_price", "special_from_date", "special_to_date", "package_weight", "package_length",
			"package_width", "package_height", "package_content", "__images__"));

	public void process(Exchange exchange) throws Exception {
		BasicDBObject inventory = exchange.getProperty("inventory", BasicDBObject.class);
		BasicDBObject channelData = (BasicDBObject) inventory.get("lazada");
		ArrayList<BasicDBObject> inventoryList = (ArrayList<BasicDBObject>) exchange.getProperty("inventoryDetails");
		exchange.setProperty("fieldsValidationSuccess", true);
		String failureReason = "";
		if (channelData.get("salePrice") != null) {
			if (channelData.get("saleStartDate") == null) {
				failureReason += "# Please fill saleStartDate \n";
			}
			if (channelData.get("saleEndDate") == null) {
				failureReason += "# Please fill saleEndDate \n";
			}
		}
		if (channelData.get("saleStartDate") != null) {
			if (channelData.get("salePrice") == null && !exchange.getProperty("hasVariants", Boolean.class)) {
				failureReason += "# Please fill salePrice \n";
			}
			if (channelData.get("saleEndDate") == null) {
				failureReason += "# Please fill saleEndDate \n";
			}
		}
		if (channelData.get("saleEndDate") != null) {
			if (channelData.get("salePrice") == null && !exchange.getProperty("hasVariants", Boolean.class)) {
				failureReason += "# Please fill salePrice \n";
			}
			if (channelData.get("saleEndDate") == null) {
				failureReason += "# Please fill saleStartDate \n";
			}
		}
		if (channelData.get("noOfItem") == null && !exchange.getProperty("hasVariants", Boolean.class)) {
			failureReason += "# Please fill noOfItem \n";
		}		
		if (channelData.get("brand") == null) {
			failureReason += "# Please fill brand \n";
		}
		if (channelData.get("packageWeight") == null) {
			failureReason += "# Please fill packageWeight \n";
		} else if (!isNumeric(channelData.getString("packageWeight"))) {
			failureReason += "# Please fill packageWeight in number \n";
		}
		if (channelData.get("shortDescription") == null) {
			failureReason += "# Please fill shortDescription \n";
		}
		if (channelData.get("categoryID") == null) {
			failureReason += "# Please fill categoryID \n";
		}
		if (channelData.get("warrantyType") == null) {
			failureReason += "# Please fill warrantyType \n";
		}
		String requestType = exchange.getProperty("requestType", String.class);
		if (requestType.equals("batchAddItem") || requestType.equals("batchEditItem")
				|| requestType.equals("batchVerifyAddItem")) {
			failureReason += compareVariationTitleAndItemSpecific(channelData, requestType, null);
		}

		String description = "";
		String categoryID = "";
		if (channelData.get("categoryID") != null) {
			categoryID = channelData.getString("categoryID");
		}
		if (channelData.get("itemDescription") != null) {
			description = channelData.getString("itemDescription");
		}
		String warningMessage = "";
		boolean isBulkImagesUpdate = false;
		if (exchange.getProperties().containsKey("isBulkImagesUpdate")
				&& exchange.getProperty("isBulkImagesUpdate", Boolean.class)) {
			isBulkImagesUpdate = true;
		}
		if (!isBulkImagesUpdate) {
			int descriptionScore = calculateDescriptionScore(exchange, description);
			if (descriptionScore < 100) {
				warningMessage = warningMessage
						+ "# WarningMessage- Minimum 1 image and 20 words are required in description to improve content score and score:"
						+ descriptionScore + "/100) \n ";
			}
		}
		String imageWarningMessage = calcuateImageScore(exchange, inventoryList);
		if (!imageWarningMessage.isEmpty()) {
			warningMessage = warningMessage + imageWarningMessage + "\n";
		}
		JSONObject warningMsg = new JSONObject();
		if (requestType.equals("batchEditItem") && exchange.getProperties().containsKey("fieldsToUpdate")) {
			List<String> fieldsToUpdate = exchange.getProperty("fieldsToUpdate", List.class);
			if (fieldsToUpdate.contains("categoryID") || fieldsToUpdate.contains("itemSpecifics")
					|| fieldsToUpdate.contains("itemDescription")) {
				failureReason = failureReason
						+ getCategoryAttributes(exchange, requestType, inventoryList, categoryID, warningMsg);
			}
		} else {
			failureReason = failureReason
					+ getCategoryAttributes(exchange, requestType, inventoryList, categoryID, warningMsg);
		}
		if (warningMsg.has("warningMessage") && !warningMsg.get("warningMessage").equals("")) {
			warningMessage += warningMsg.getString("warningMessage");
		}
		if (!warningMessage.isEmpty() && !isBulkImagesUpdate) {
			exchange.setProperty("warningMessage", warningMessage);
		}
		if (exchange.getProperties().containsKey("packageFailureReason")
				&& !exchange.getProperty("packageFailureReason", String.class).isEmpty()) {
			failureReason += exchange.getProperty("packageFailureReason", String.class);
		}
		if (!failureReason.isEmpty()) {
			exchange.setProperty("isPostingSuccess", false);
			exchange.setProperty("fieldsValidationSuccess", false);
			exchange.setProperty("failureReason", failureReason);
		}
	}

	private String getCategoryAttributes(Exchange exchange, String requestType, ArrayList<BasicDBObject> inventoryList,
			String categoryID, JSONObject warningMsg) throws JSONException, IOException {
		Map<String, String> header = new HashMap<String, String>();
		String erroMessage = "";
		String warningMessage = "";
		header.put("Content-Type", "application/json");
		header.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		header.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		String totalURL = Config.getConfig().getSIAListingLookupServerURL() + "/lazada/category/"
				+ exchange.getProperty("countryCode", String.class) + "/" + categoryID;
		JSONObject lookupJSON = HttpsURLConnectionUtil.doGet(totalURL, header);
		if (lookupJSON.getInt("httpCode") == HttpStatus.OK_200) {
			JSONObject payLoadJSON = new JSONObject(lookupJSON.getString("payload"));
			JSONArray attributes = payLoadJSON.getJSONArray("attributes");
			ArrayList<String> mandatoryFieldTitleList = new ArrayList<String>();
			ArrayList<String> keyPropFieldTitleList = new ArrayList<String>();
			HashMap<String, JSONObject> categoryMap = new HashMap<String, JSONObject>();
			String removeKeyPropFieldTitleList = Config.getConfig().getRemoveKeyPropFieldTitleList();
			exchange.setProperty("removeKeyPropFieldTitleList", removeKeyPropFieldTitleList);
			buildmandatoryField(attributes, mandatoryFieldTitleList, keyPropFieldTitleList, categoryMap);
			for (BasicDBObject inventory : inventoryList) {
				BasicDBObject channel = (BasicDBObject) inventory.get("lazada");
				if (channel.containsField("itemSpecifics")) {
					BasicDBList itemSpecifics = (BasicDBList) channel.get("itemSpecifics");
					for (int i = 0; i < itemSpecifics.size(); i++) {
						BasicDBObject itemSpecific = (BasicDBObject) itemSpecifics.get(i);
						for (int j = 0; j < attributes.length(); j++) {
							JSONObject item = attributes.getJSONObject(j);
							String title = itemSpecific.getString("title").contains(".")
									? itemSpecific.getString("title").split("[.]")[1]
									: itemSpecific.getString("title");
							if (title.equals(item.getString("name"))) {
								if (!itemSpecific.getString("title").startsWith(item.getString("attribute_type"))) {
									warningMessage += "# " + itemSpecific.getString("title") + " should be like "
											+ item.getString("attribute_type") + "." + title + " \n ";
								}
								break;
							}
						}
					}
				}
			}
			warningMsg.put("warningMessage", warningMessage);
			erroMessage = validateItemSpecfic(exchange, requestType, inventoryList, mandatoryFieldTitleList, keyPropFieldTitleList,
					categoryMap);
		}
		return erroMessage;
	}

	public void buildmandatoryField(JSONArray attributes, ArrayList<String> mandatoryFieldTitleList,
			ArrayList<String> keyPropFieldTitleList, HashMap<String, JSONObject> categoryMap) throws JSONException {
		for (int i = 0; i < attributes.length(); i++) {
			JSONObject item = attributes.getJSONObject(i);
			if (item.has("is_mandatory") && item.getInt("is_mandatory") == 1) {
				if (!LAZADA_MANDATORY_FIELDS.contains(item.getString("name"))) {
					mandatoryFieldTitleList.add(item.getString("attribute_type") + "." + item.getString("name"));
				}
			} else if (item.has("advanced")) {
				JSONObject advancedObject = item.getJSONObject("advanced");
				// This validation is for key property mandatory to improve content score
				if (advancedObject.has("is_key_prop") && advancedObject.getInt("is_key_prop") == 1 && !Arrays
						.asList(Config.getConfig().getRemoveKeyPropFieldTitleList().split(",")).contains(item.getString("name").replaceAll("[^a-zA-Z0-9]", ""))) {
					if (!LAZADA_MANDATORY_FIELDS.contains(item.getString("name"))) {
						keyPropFieldTitleList.add(item.getString("attribute_type") + "." + item.getString("name"));
					}
				}
			}
			String keyName = item.getString("attribute_type") + "." + item.getString("name");
			categoryMap.put(keyName.replaceAll("[^a-zA-Z0-9]", ""), item);
		}

	}

	public String validateItemSpecfic(Exchange exchange, String requestType, ArrayList<BasicDBObject> inventoryList,
			ArrayList<String> mandatoryFieldTitleList, ArrayList<String> keyPropFieldTitleList,
			HashMap<String, JSONObject> categoryMap) throws JSONException {
		String errorMessage = "";
		for (BasicDBObject inventory : inventoryList) {
			String SKU = inventory.getString("SKU");
			BasicDBObject lazadaObject = (BasicDBObject) inventory.get("lazada");
			ArrayList<String> missingFiled = new ArrayList<String>();
			ArrayList<String> keyPropMissingFiled = new ArrayList<String>();
			ArrayList<String> dbItemSpecificTitleList = new ArrayList<String>();
			HashMap<String, JSONArray> itemSpecificsMap = new HashMap<String, JSONArray>();
			if (lazadaObject.containsField("itemSpecifics")) {
				ArrayList<BasicDBObject> itemSpec = (ArrayList<BasicDBObject>) lazadaObject.get("itemSpecifics");
				for (BasicDBObject item : itemSpec) {
					JSONObject itemObject = LazadaUtil.parseToJsonObject((DBObject) item);
					dbItemSpecificTitleList.add(itemObject.getString("title"));
					JSONArray itemArray = itemObject.getJSONArray("names");
					itemSpecificsMap.put(itemObject.getString("title"), itemArray);
				}
			}

			if (exchange.getProperty("hasVariants", Boolean.class)) {// Variant
				if (lazadaObject.containsField("variantDetails")) {
					missingFiled = findMissing(mandatoryFieldTitleList, dbItemSpecificTitleList, "sku.", categoryMap);
					keyPropMissingFiled = findMissing(keyPropFieldTitleList, dbItemSpecificTitleList, "sku.", categoryMap);
				} else {
					missingFiled = findMissing(mandatoryFieldTitleList, dbItemSpecificTitleList, "normal.", categoryMap);
					keyPropMissingFiled = findMissing(keyPropFieldTitleList, dbItemSpecificTitleList, "normal.", categoryMap);
				}

			} else {
				missingFiled = findMissing(mandatoryFieldTitleList, dbItemSpecificTitleList, "", categoryMap);
				keyPropMissingFiled = findMissing(keyPropFieldTitleList, dbItemSpecificTitleList, "", categoryMap);
			}
			if (missingFiled.size() > 0) {
				errorMessage = errorMessage + "# " + String.join(",", missingFiled)
						+ " are field(s) mandatory for SKU: " + SKU + "\n";
			}
			// This validation is for key property mandatory to improve content score
			if (keyPropMissingFiled.size() > 0) {
				errorMessage = errorMessage + "# " + String.join(",", keyPropMissingFiled)
						+ " field(s) are required for improve content score for SKU: " + SKU + "\n";
			}
			errorMessage = errorMessage + checkItemKeyandValue(categoryMap, itemSpecificsMap, SKU);
			if (requestType.equals("batchAddItem") || requestType.equals("batchEditItem")
					|| requestType.equals("batchVerifyAddItem") || requestType.equals("addVariant")) {
				errorMessage += compareVariationTitleAndItemSpecific(lazadaObject, requestType, SKU);
			}
		}
		return errorMessage;
	}

	public ArrayList<String> findMissing(ArrayList<String> mandatoryFieldTitleList,
			ArrayList<String> dbItemSpecificTitleList, String type, HashMap<String, JSONObject> categoryMap) throws JSONException {
		int n = mandatoryFieldTitleList.size();
		int m = dbItemSpecificTitleList.size();
		ArrayList<String> missedTitles = new ArrayList<String>();
		for (int i = 0; i < n; i++) {
			int j;
			boolean matched = false;
			for (j = 0; j < m; j++) {
				if (mandatoryFieldTitleList.get(i).startsWith(type)) {
					if (mandatoryFieldTitleList.get(i).equals(dbItemSpecificTitleList.get(j))) {
						matched = true;
						break;
					}
				}
			}
			if (!matched && mandatoryFieldTitleList.get(i).startsWith(type)) {
				missedTitles.add(mandatoryFieldTitleList.get(i));
			}
		}
		if (categoryMap.containsKey("normaldeliveryoptioneconomy")) {
			JSONObject categoryObejct = categoryMap.get("normaldeliveryoptioneconomy");
			int isMandatory = categoryObejct.getInt("is_mandatory");
			if (!dbItemSpecificTitleList.contains("normal.delivery_option_economy") && isMandatory == 1) {
				missedTitles.add("normal.delivery_option_economy");
			}
		}
		return missedTitles;
	}

	public String checkItemKeyandValue(HashMap<String, JSONObject> categoryMap,
			HashMap<String, JSONArray> itemSpecificsMap, String SKU) throws JSONException {
		StringBuffer errorMessage = new StringBuffer();
		for (Entry<String, JSONArray> entry : itemSpecificsMap.entrySet()) {
			String key = entry.getKey();
			JSONArray value = entry.getValue();
			if (key.startsWith("sku.") || key.startsWith("normal.")) {
				if (categoryMap.containsKey(key.replaceAll("[^a-zA-Z0-9]", ""))) {
					JSONObject categoryObejct = categoryMap.get(key.replaceAll("[^a-zA-Z0-9]", ""));
					JSONArray options = categoryObejct.getJSONArray("options");
					ArrayList<String> optionArray = new ArrayList<String>();
					for (int i = 0; i < options.length(); i++) {
						optionArray.add(options.getJSONObject(i).getString("name"));
					}
					String inputType = categoryObejct.getString("input_type");
					int isMandatory = categoryObejct.getInt("is_mandatory");
					if (inputType.equals("numeric")) {
						if (value.length() > 0) {
							if (!isNumeric(value.getString(0))) {
								errorMessage.append("# " + key + "='" + value.getString(0)
										+ "' please fill valid numeric value for SKU: " + SKU + "\n");
							}
						} else {
							if (isMandatory == 1) {
								errorMessage.append(
										"# " + key + " filed can't be empty.Please fill valid numeric value for SKU: "
												+ SKU + "\n");
							}
						}
					}
					if (inputType.equals("singleSelect")) {
						if (value.length() > 0) {
							if (!optionArray.contains(value.getString(0))) {
								errorMessage.append("# " + key + "='" + value.getString(0)
										+ "' please fill valid option value for SKU: " + SKU + "\n");
							}
						} else {
							if (isMandatory == 1) {
								errorMessage.append(
										"# " + key + " filed can't be empty.Please fill valid option value for SKU: "
												+ SKU + "\n");
							}
						}
					}
					if (inputType.equals("multiSelect")) {
						if (value.length() > 0) {
							String[] fullString = value.getString(0).split(",");
							for (String s : fullString) {
								if (!optionArray.contains(s)) {
									errorMessage.append("# " + key + "='" + s
											+ "' please fill valid option value for SKU: " + SKU + "\n");
								}
							}
						} else {
							if (isMandatory == 1) {
								errorMessage.append(
										"# " + key + " filed can't be empty.Please fill valid option value for SKU: "
												+ SKU + "\n");
							}
						}
					}
					if (inputType.equals("text")) {
						if (isMandatory == 1 && (value.length() == 0 || value.getString(0).isEmpty())) {
							errorMessage.append("# " + key + " filed can't be empty for SKU : " + SKU + "\n");
						}
					}
				} else {
					errorMessage.append("# " + key + " is invalid key for SKU: " + SKU + "\n");
				}
			}

		}
		return errorMessage.toString();
	}

	public boolean isNumeric(String strNum) {
		return strNum.matches("-?\\d+(\\.\\d+)?");
	}

	public int calculateDescriptionScore(Exchange exchange, String description) {
		int score = 10;
		if (description != null && description != "") {
			Pattern removeTags = Pattern.compile("<(?:\"[^\"]*\"['\"]*|'[^']*'['\"]*|[^'\">])>");
			String withoutStyle = description.replaceAll("<style([\\s\\S]+?)</style>", "");
			Pattern imageTags = Pattern.compile("<img .+?>", Pattern.MULTILINE);
			Matcher tagMatcher = removeTags.matcher(withoutStyle);
			Matcher imageMatcher = imageTags.matcher(description);
			String withOutHtmlTag = tagMatcher.replaceAll("").trim();
			withOutHtmlTag = withOutHtmlTag.replaceAll("\\s+", " ");//remove extra white space between two words
			String[] wordsCount = withOutHtmlTag.split("\\s+");
			int count = 0;
			Set<String> descriptionImageSet = new HashSet<String>();
			while (imageMatcher.find()) {
				String imageURL = imageMatcher.group();
				imageURL = imageURL.substring(imageURL.indexOf("src=") + 5);
				int singleQuoteIndex  = imageURL.indexOf("'");
				int doubleQuoteIndex = imageURL.indexOf("\"");
				int lastIndex = 0;
				if (singleQuoteIndex > 0 && doubleQuoteIndex > 0 && singleQuoteIndex <= doubleQuoteIndex) {
					lastIndex = singleQuoteIndex;
				} else if (doubleQuoteIndex > 0) {
					lastIndex = doubleQuoteIndex;
				}
				imageURL = imageURL.substring(0, lastIndex);
				descriptionImageSet.add(imageURL);
				count++;
			}
			if (descriptionImageSet.size() > 0) {
				exchange.setProperty("descriptionImageList", new ArrayList<String>(descriptionImageSet));
			}
			if (wordsCount.length >= 20 && count >= 1) {
				score = 100;
			} else if (wordsCount.length < 20 && count >= 1) {
				score = 80;
			} else if (wordsCount.length <= 20 && count <= 0) {
				score = 20;
			}
			if (wordsCount.length > 20 && count <= 0) {
				score = 0;
			}
		}
		return score;

	}

	public String calcuateImageScore(Exchange exchange, ArrayList<BasicDBObject> inventoryList) {
		ArrayList<String> missingSKU = new ArrayList<String>();
		String packageFailureMsg = "";
		for (BasicDBObject inventory : inventoryList) {
			String SKU = inventory.getString("SKU");
			BasicDBObject channel = (BasicDBObject) inventory.get("lazada");
			if (SKU.contains("-") || (!SKU.contains("-") && !exchange.getProperty("hasVariants", Boolean.class))) {
				ArrayList<String> imageURI = (ArrayList<String>) channel.get("imageURI");
				if (imageURI != null) {
					if (imageURI.size() < 3) {
						missingSKU.add(SKU);
					}
				}
			}
			packageFailureMsg += validatePackageDetails(SKU, channel, packageFailureMsg);
		}
		exchange.setProperty("packageFailureReason", packageFailureMsg);
		if (missingSKU.size() > 0) {
			return "# WarningMessage - Minimum 3 images are required to improve content score for SKU(s):"
					+ String.join(",", missingSKU);
		} else {
			return "";
		}
	}
	
	private String validatePackageDetails(String SKU, BasicDBObject channelData, String packageFailureMsg) {
		String failureMessage = "";
		if (channelData.get("packageWeight") == null && !packageFailureMsg.contains("packageWeight")) {
			failureMessage += "# Please fill packageWeight \n";
		}
		if (channelData.get("packageHeight") == null && !packageFailureMsg.contains("packageHeight")) {
			failureMessage += "# Please fill packageHeight \n";
		}
		if (channelData.get("packageLength") == null && !packageFailureMsg.contains("packageLength")) {
			failureMessage += "# Please fill packageLength \n";
		}
		if (channelData.get("packageWidth") == null && !packageFailureMsg.contains("packageWidth")) {
			failureMessage += "# Please fill packageWidth \n";
		}
		return failureMessage;
	}

	public String compareVariationTitleAndItemSpecific(BasicDBObject channelData, String requestType, String SKU) {
		String failureReason = "";
		if (channelData.containsField("variantDetails")) {
			if (channelData.containsField("itemSpecifics")) {
				BasicDBList variants = new BasicDBList();
				if (channelData.containsField("variants")) {
					variants = (BasicDBList) channelData.get("variants");
				} else {
					variants = (BasicDBList) channelData.get("variantDetails");
				}
				BasicDBList itemSpecifics = (BasicDBList) channelData.get("itemSpecifics");
				ArrayList<String> variantsTitles = new ArrayList<String>();
				List<String> variantNames = new ArrayList<String>();
				ArrayList<String> itemSpecificsTitles = new ArrayList<String>();
				for (int i = 0; i < variants.size(); i++) {
					BasicDBObject variant = (BasicDBObject) variants.get(i);
					variantsTitles.add(variant.getString("title"));
					if (variant.containsField("names")) {
						variantNames.add(variant.getString("names"));
					} else if (variant.containsField("name")) {
						variantNames.add(variant.getString("name"));
					}
				}
				for (int i = 0; i < itemSpecifics.size(); i++) {
					BasicDBObject itemSpecific = (BasicDBObject) itemSpecifics.get(i);
					String variant = itemSpecific.getString("title");
					if (variant.contains("sku.")) {
						itemSpecificsTitles.add(variant.split("sku.")[1]);
						BasicDBList itemSpecNames = (BasicDBList) itemSpecific.get("names");
						for (int j = 0; j < itemSpecNames.size(); j++) {
							if (variantNames.indexOf(itemSpecNames.get(j)) != -1) {
								variantNames.remove(variantNames.indexOf(itemSpecNames.get(j)));
							}
						}
					}
				}
				if (variantsTitles.size() != itemSpecificsTitles.size()) {
					failureReason += "# The variations should be same as the variations in itemSpecifics. \n";
				}
				for (int i = 0; i < itemSpecificsTitles.size(); i++) {
					if (!variantsTitles.contains(itemSpecificsTitles.get(i))) {
						failureReason += "# " + itemSpecificsTitles.get(i) + " is missing from variations. \n";
					}
				}
				if (variantNames.size() != 0) {
					failureReason += "# Variant details and item specifics attribute doesn't match for : "+ SKU + " \n";
				}
			}
		}
		return failureReason;
	}
	
}
