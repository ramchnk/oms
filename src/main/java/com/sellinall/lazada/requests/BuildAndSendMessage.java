package com.sellinall.lazada.requests;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;

public class BuildAndSendMessage implements Processor {

	static Logger log = Logger.getLogger(BuildAndSendMessage.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accountNumber = (String) exchange.getProperty("accountNumber");
		String nickNameID = (String) exchange.getProperty("nickNameID");
		String accessToken = (String) exchange.getProperty("accessToken");
		JSONObject contentObject = exchange.getProperty("content", JSONObject.class);
		String messageType = exchange.getProperty("messageType", String.class);
		String url = (String) exchange.getProperty("hostURL");
		String errorMessage = "";
		JSONObject resultObj = new JSONObject();
		exchange.setProperty("isMessageSent", false);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("session_id", exchange.getProperty("conversationID", String.class));
		headers.put("access_token", accessToken);

		String msgText = "";
		if (contentObject.has("text") && !contentObject.getString("text").isEmpty()) {
			headers.put("txt", contentObject.getString("text"));
			msgText = contentObject.getString("text");
		}
		String queryParam = "&session_id=" + exchange.getProperty("conversationID", String.class);
		if (!msgText.isEmpty()) {
			queryParam += "&txt=" + URLEncoder.encode(msgText, "UTF-8");
		}
		if (contentObject.has("imageUrl")) {
			headers.put("img_url", contentObject.getString("imageUrl"));
			queryParam += "&img_url=" + contentObject.getString("imageUrl");
		}
		if (messageType.equals("image")) {
			String filename = exchange.getProperty("uploadedImageURL", String.class);
			if (filename != null) {
				URL url1 = new URL(filename);
				BufferedImage image = ImageIO.read(url1);
				int height = image.getHeight();
				int width = image.getWidth();
				queryParam += "&height=" + height;
				queryParam += "&width=" + width;
				headers.put("height", String.valueOf(height));
				headers.put("width", String.valueOf(width));
			} else {
				log.error("Error occured while uploading image for accountNumber : " + accountNumber
						+ " and nickNameID : " + nickNameID);
				resultObj.put("status", "failure");
				resultObj.put("errorMessage", "Filed to upload image");
				exchange.setProperty("response", resultObj);
				return;
			}
			headers.put("template_id", "3");
			queryParam += "&template_id=3";
		} else if (messageType.equals("text")) {
			headers.put("template_id", "1");
			queryParam += "&template_id=1";
		} else if (messageType.equals("order")) {
			headers.put("template_id", "10007");
			queryParam += "&template_id=10007";
			queryParam += "&order_id=" + contentObject.getString("order");
			headers.put("order_id", contentObject.getString("order"));
		} else if (messageType.equals("item")) {
			String itemId = getItemIdFromInventory(contentObject.getString("item"), accountNumber, nickNameID);
			headers.put("template_id", "10006");
			queryParam += "&template_id=10006";
			queryParam += "&item_id=" + itemId;
			headers.put("item_id", itemId);
		} else if (messageType.equals("voucher")) {
			String voucherId = contentObject.getString("voucher");
			headers.put("template_id", "10008");
			queryParam += "&template_id=10008";
			queryParam += "&promotion_id=" + voucherId;
			headers.put("promotion_id", voucherId);
		}
		String response = NewLazadaConnectionUtil.callAPI(url, "/im/message/send", accessToken, headers, "", queryParam,
				"POST", Config.getConfig().getLazadaChatClientID(), Config.getConfig().getLazadaChatClientSecret());
		JSONObject result = new JSONObject(response);
		String status = "failure";
		try {
			if (result.has("data")) {
				JSONObject payload = new JSONObject(result.getString("data"));
				if (result.has("err_code") && !result.getString("err_code").equals("0")) {
					if (result.has("error")) {
						errorMessage = result.getString("error");
					} else {
						errorMessage = "Getting failure response from Lazada.";
					}
					log.error("Error occurred while posting conversation msg for accountNumber : " + accountNumber
							+ " and nickNameID : " + nickNameID + " and queryParam is " + queryParam
							+ " and response is " + result.toString());
				} else {
					status = "success";
					exchange.setProperty("isMessageSent", true);
					resultObj.put("response", payload);
				}
			} else {
				errorMessage = "Lazada server not responding please contact to support team";
				log.error("Lazada server not responding while posting conversation  msg for accountNumber : "
						+ accountNumber + " and nickNameID : " + nickNameID + " and queryParam is " + queryParam
						+ " and response is: " + result.toString());
			}
		} catch (Exception e) {
			errorMessage = "internal error please contact to support team";
			log.error("Internal error occurred while posting conversation  msg for accountNumber : " + accountNumber
					+ " and nickNameID : " + nickNameID + " and queryParam is " + queryParam + " and response is: ", e);
		}
		resultObj.put("status", status);
		if (!errorMessage.equals("")) {
			resultObj.put("errorMessage", errorMessage);
		}
		exchange.setProperty("response", resultObj);
	}

	private String getItemIdFromInventory(String sku, String accountNumber, String nickNameID)
			throws IOException, JSONException {
		sku = sku.split("-")[0];
		Map<String, String> header = new HashMap<String, String>();
		header.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		header.put("Content-Type", "application/json");
		header.put("accountNumber", accountNumber);

		JSONObject inventoryObj = HttpsURLConnectionUtil.doGet(Config.getConfig().getInventoryUrl() + "/inv?SKU=" + sku,
				header);
		JSONObject responsePayload = new JSONObject(inventoryObj.getString("payload"));
		JSONArray inventoryDetailArray = responsePayload.getJSONArray(nickNameID.split("-")[0]);
		JSONObject inventoryDetailObj = (JSONObject) inventoryDetailArray.get(0);
		return inventoryDetailObj.getString("itemID");
	}

}
