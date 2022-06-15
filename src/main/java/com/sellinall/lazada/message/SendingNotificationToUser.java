package com.sellinall.lazada.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import com.mongodb.DBObject;
import com.sellinall.util.enums.UserMessageName;

public class SendingNotificationToUser implements Processor {
	static Logger log = Logger.getLogger(SendingNotificationToUser.class.getName());

	public void process(Exchange exchange) throws Exception {
		int totalNumberOfPromotions = exchange.getProperty("totalNumberOfPromotions", Integer.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		DBObject channelObj = exchange.getProperty("userChannel", DBObject.class);
		DBObject nickName = (DBObject) channelObj.get("nickName");
		String nickNameID = (String) nickName.get("id");
		String nickNameValue = (String) nickName.get("value");
		String fullNickName = nickNameID.split("-")[0] + "-" + nickNameValue;
		String failureReason = "";
		if (exchange.getProperties().containsKey("failureReason")) {
			failureReason = exchange.getProperty("failureReason", String.class);
		}
		JSONObject outBody = new JSONObject();
		JSONObject message = new JSONObject();
		message.put("accountNumber", accountNumber);
		message.put("nickNameID", nickNameID);
		message.put("nickName", fullNickName);
		message.put("totalNumberOfPromotions", totalNumberOfPromotions);
		message.put("PromotionType", "Flexi Combo");
		if (!failureReason.isEmpty()) {
			message.put("failureReason", failureReason);
		}
		outBody.put("accountNumber", accountNumber);
		outBody.put("userMessageName", UserMessageName.PROMOTION_IMPORT_STATUS.toString());
		outBody.put("message", message);
		exchange.getOut().setBody(outBody);
	}
}
