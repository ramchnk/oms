package com.sellinall.lazada.response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class ProcessPromotionUpdateResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessPromotionUpdateResponse.class.getName());

	public void process(Exchange exchange) throws Exception {
		Integer httpStatusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
		String responseString = exchange.getIn().getBody(String.class);
		try {
			if (httpStatusCode != HttpStatus.SC_OK && !responseString.startsWith("{")) {
				log.error("promotionData update failed  for accountNumber : "
						+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
						+ exchange.getProperty("nickNameID", String.class) + ", requestID : "
						+ exchange.getProperty("requestID", String.class) + ", action : "
						+ exchange.getProperty("action", String.class) + " and promotionID : "
						+ exchange.getProperty("promotionID", String.class) + " and response is" + responseString);
				return;
			}
			JSONObject responsePayload = new JSONObject(responseString);
			if (responsePayload.has("response") && responsePayload.getString("response").equals("success")) {
				log.info("promotionData updated for accountNumber : "
						+ exchange.getProperty("accountNumber", String.class) + ", nickNameID : "
						+ exchange.getProperty("nickNameID", String.class) + ", action : "
						+ exchange.getProperty("action", String.class) + " and promotionID : "
						+ exchange.getProperty("promotionID", String.class));
				return;
			} else {
				log.error("promotionData update failed  for accountNumber : "
						+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
						+ exchange.getProperty("nickNameID", String.class) + ", action : "
						+ exchange.getProperty("action", String.class) + " and promotionID : "
						+ exchange.getProperty("promotionID", String.class) + " and response is" + responseString);
				return;
			}
		} catch (Exception e) {
			log.error("promotionData update failed  for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + ", action : "
					+ exchange.getProperty("action", String.class) + " and promotionID : "
					+ exchange.getProperty("promotionID", String.class) + " and response is" + responseString);
			e.printStackTrace();
		}
	}

}
