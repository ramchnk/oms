package com.sellinall.lazada.message;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.enums.SIAInquiryMessageType;

public class ConstructInquiryMessage implements Processor {
	static Logger log = Logger.getLogger(ConstructInquiryMessage.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject message = createMessage(exchange);
		if (message.length() != 0) {
			exchange.setProperty("isEligibleToPublishMsg", true);
		}
		exchange.getOut().setBody(message);
	}

	@SuppressWarnings("unchecked")
	private JSONObject createMessage(Exchange exchange) throws Exception {
		JSONObject message = exchange.getIn().getBody(JSONObject.class);
		BasicDBObject accountDetails = (BasicDBObject) LazadaUtil.getAccountDetails(message.getString("seller_id"));
		if (accountDetails == null) {
			exchange.setProperty("isEligibleToPublishMsg", false);
			return new JSONObject();
		}
		exchange.setProperty("isEligibleToPublishMsg", true);

		List<DBObject> lazada = (List<DBObject>) accountDetails.get("lazada");
		DBObject lazadaObj = lazada.get(0);
		DBObject nickNameObj = (DBObject) lazadaObj.get("nickName");
		String countryCode = lazadaObj.get("countryCode").toString();
		JSONObject data = message.getJSONObject("data");

		JSONObject inquiryMessage = new JSONObject();
		inquiryMessage.put("accountNumber", accountDetails.getString("_id"));
		inquiryMessage.put("merchantID", accountDetails.getString("merchantID"));
		inquiryMessage.put("countryCode", countryCode);
		inquiryMessage.put("nickNameID", nickNameObj.get("id").toString());
		inquiryMessage.put("conversationID", data.getString("session_id"));
		inquiryMessage.put("read", false);
		inquiryMessage.put("isAnswered", false);

		JSONObject messageBody = new JSONObject();
		if (data.has("send_time")) {
			messageBody.put("time", data.getString("send_time"));
		}
		if (data.has("content") && data.has("template_id")) {
			if (data.getString("content").contains("recallContent")
					|| data.getString("content").contains("Chat session expired")) {
				exchange.setProperty("isEligibleToPublishMsg", false);
				return new JSONObject();
			}
			messageBody.put("message", data.getString("content"));
			messageBody.put("messagetype", getMessageType(data.getString("template_id")));
			inquiryMessage.put("subject", getsubject(new JSONObject(data.getString("content")),data.getString("template_id")));
		}
		if (data.has("message_id")) {
			messageBody.put("messageID", data.getString("message_id"));
		}
		if (data.has("to_account_type")) {
			messageBody.put("sender", getSenderDetails(data.getString("to_account_type")));
			if(data.getString("to_account_type").equals("1")) {
				inquiryMessage.put("isAnswered", true);
			}
		}
		if (data.has("status") && data.getInt("status") == 1) {
			messageBody.put("status", data.getInt("status"));
		}

		JSONArray body = new JSONArray();
		body.put(messageBody);
		inquiryMessage.put("body", body);
		inquiryMessage.put("lastInquiryUpdated", System.currentTimeMillis() / 1000L);
		inquiryMessage.put("requestType", "updateInquiry");
		return inquiryMessage;
	}


	private String getsubject(JSONObject content, String messageType) throws JSONException {
		switch (messageType) {
		case "1":
			return content.getString("txt");
		case "3":
			return "[" + SIAInquiryMessageType.IMAGE.toString() +"]";
		case "4":
			return "[" + SIAInquiryMessageType.STICKER.toString() +"]";
		case "10006":
			return "[ Product Details ]";
		case "10007":
			return "[ Order Details ]";
		case "10008":
			return "[ Voucher ]";
		default:
			return "";
		}
	}

	private String getMessageType(String messageType) {
		switch (messageType) {
		case "1":
			return SIAInquiryMessageType.TEXT.toString();
		case "2":
			return SIAInquiryMessageType.CHANNEL_TRIGGERED.toString();
		case "3":
			return SIAInquiryMessageType.IMAGE.toString();
		case "4":
			return SIAInquiryMessageType.STICKER.toString();
		case "10006":
			return SIAInquiryMessageType.INVENTORY.toString();
		case "10007":
			return SIAInquiryMessageType.ORDER.toString();
		case "10008":
			return SIAInquiryMessageType.VOUCHER.toString();
		case "10010":
			return SIAInquiryMessageType.STORE_FOLLOW.toString();
		case "10011":
			return SIAInquiryMessageType.RETURN_ORDER.toString();
		case "10015":
			return SIAInquiryMessageType.AUTO_REPLY.toString();
		default:
			return "";
		}
	}

	private String getSenderDetails(String accountType) {
		if (accountType.equals("1")) {
			return "seller";
		} else if (accountType.equals("2")) {
			return "buyer";
		}
		return "";

	}
}
