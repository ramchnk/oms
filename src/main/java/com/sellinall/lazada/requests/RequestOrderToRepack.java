package com.sellinall.lazada.requests;

import java.net.URLEncoder;
import java.util.HashMap;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;
import com.sellinall.util.enums.OrderUpdateStatus;
import com.sellinall.util.enums.SIAShippingStatus;

public class RequestOrderToRepack implements Processor {
	static Logger log = Logger.getLogger(RequestOrderToRepack.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		String orderBody = exchange.getIn().getBody().toString();
		JSONObject order = new JSONObject(orderBody);
		String response = null;
		exchange.setProperty("isRepackedSuccessfully", false);
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		String packageID = exchange.getProperty("packageID", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("access_token", accessToken);
		map.put("package_id", packageID);
		String queryParams = "&package_id=" + URLEncoder.encode(packageID, "UTF-8");
		String orderID = order.getString("orderID");
		JSONObject serviceResponse = new JSONObject();
		exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
		try {
			String clientID = Config.getConfig().getLazadaClientID();
			String clientSecret = Config.getConfig().getLazadaClientSecret();
			response = NewLazadaConnectionUtil.callAPI(hostURL, "/order/repack", accessToken, map, "", queryParams,
					"POST", clientID, clientSecret);
			serviceResponse = new JSONObject(response);
			if (serviceResponse.has("code") && !serviceResponse.getString("code").equals("0")) {
				log.error("repack request failed for nickNameId:" + exchange.getProperty("nickNameID", String.class)
						+ " for accountNumber: " + exchange.getProperty("accountNumber", String.class) + ", orderID : "
						+ orderID + " request" + queryParams.toString() + "  and response: " + response);

				if (serviceResponse.has("message")) {
					exchange.setProperty("failureReason", serviceResponse.getString("message"));
				}
				return;
			}
			if (!serviceResponse.has("data")) {
				log.error("repack request failed for nickNameId:" + exchange.getProperty("nickNameID", String.class)
						+ " for accountNumber: " + exchange.getProperty("accountNumber", String.class) + ", orderID : "
						+ orderID + " request" + queryParams.toString() + "  and response: " + response);
				return;
			}
			log.info("repack requested for accountNumber: " + exchange.getProperty("accountNumber", String.class)
					+ ", nickNameID: " + exchange.getProperty("nickNameID", String.class) + ", orderID: " + orderID
					+ " and response: " + serviceResponse);
			exchange.setProperty("updateStatus", OrderUpdateStatus.COMPLETE.toString());
			exchange.setProperty("orderUpdateStatus", SIAShippingStatus.READY_TO_SHIP.toString());
		} catch (Exception e) {
			log.error("Error occurred during repack request for nickNameId:"
					+ exchange.getProperty("nickNameID", String.class) + " for accountNumber: "
					+ exchange.getProperty("accountNumber", String.class) + ", orderID: " + orderID + " and response: "
					+ response);
			e.printStackTrace();
		}
	}

}
