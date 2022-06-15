package com.sellinall.lazada.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.DBObject;
import com.mudra.sellinall.config.APIUrlConfig;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.EncryptionUtil;

public class ProcessUserDBData implements Processor {
	static Logger log = Logger.getLogger(ProcessUserDBData.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject user = LazadaUtil.parseToJsonObject((DBObject) exchange.getIn().getBody());
		JSONArray channelArray = user.getJSONArray("lazada");

		for (int i = 0; i < channelArray.length(); i++) {
			JSONObject userChannel = (JSONObject) channelArray.get(i);
			JSONObject nickName = userChannel.getJSONObject("nickName");
			String id = nickName.getString("id");
			if (id.equals(exchange.getProperty("nickNameID"))) {
				exchange.setProperty("userChannel", userChannel);
				exchange.setProperty("merchantID", user.get("merchantID"));
				exchange.setProperty("getAccessToken", false);
				String countryCode = userChannel.getString("countryCode");
				exchange.setProperty("countryCode", countryCode);
				boolean isGlobalAccount = false;
				if (countryCode.equalsIgnoreCase("GLOBAL")) {
					isGlobalAccount = true;
				} else {
					exchange.setProperty("countryCode", countryCode);
				}
				exchange.setProperty("isGlobalAccount", isGlobalAccount);
				String preferredLogistic = "";
				JSONArray shippingServiceProviderList = new JSONArray();
				if (isGlobalAccount) {
					if (exchange.getProperties().containsKey("order")) {
						JSONObject order = (JSONObject) exchange.getProperty("order");
						String currencyCode = "";
						if (order.has("orderAmount")) {
							currencyCode = order.getJSONObject("orderAmount").getString("currencyCode");
						} else if (order.has("orderSoldAmount")) {
							currencyCode = order.getJSONObject("orderSoldAmount").getString("currencyCode");
						}
						countryCode = LazadaUtil.currencyToCountryCodeMap.get(currencyCode);
					} else if (exchange.getProperties().containsKey("syncCountryCode")) {
						countryCode = exchange.getProperty("syncCountryCode", String.class);
					}
					if (userChannel.has("shippingServiceProvider") && !countryCode.equals("GLOBAL")) {
						JSONObject shippingServiceProvider = userChannel.getJSONObject("shippingServiceProvider");
						if (shippingServiceProvider.has(countryCode)) {
							shippingServiceProviderList = shippingServiceProvider.getJSONArray(countryCode);
						}
					}
				} else if (userChannel.has("shippingServiceProvider")) {
					shippingServiceProviderList = userChannel.getJSONArray("shippingServiceProvider");
				}
				if (userChannel.has("isEncodeShippingLabel")) {
					exchange.setProperty("isEncodeShippingLabel", userChannel.getBoolean("isEncodeShippingLabel"));
				}
				for (int j = 0; j < shippingServiceProviderList.length(); j++) {
					JSONObject shippingServiceProvider = shippingServiceProviderList.getJSONObject(j);
					if (shippingServiceProvider.has("preferred") && shippingServiceProvider.getBoolean("preferred")) {
						preferredLogistic = shippingServiceProvider.has("Name")
								? shippingServiceProvider.getString("Name")
								: shippingServiceProvider.getString("name");
						break;
					}
				}
				if (preferredLogistic.isEmpty() && shippingServiceProviderList.length() > 0) {
					JSONObject shippingServiceProvider = shippingServiceProviderList.getJSONObject(0);
					preferredLogistic = shippingServiceProvider.has("Name")
							? shippingServiceProvider.getString("Name")
							: shippingServiceProvider.getString("name");
				}
				exchange.setProperty("preferredLogistic", preferredLogistic);
				if (userChannel.has("postHelper")) {
					String hostURL= "";
					JSONObject posthelper = userChannel.getJSONObject("postHelper");
					String refreshToken = posthelper.getString("refreshToken");
					exchange.setProperty("refreshToken", refreshToken);
					if (!isGlobalAccount) {
						hostURL = APIUrlConfig.getNewAPIUrl(countryCode.toUpperCase());
					} else if (exchange.getProperties().containsKey("syncCountryCode")) {
						countryCode = exchange.getProperty("syncCountryCode", String.class);
						hostURL = APIUrlConfig.getNewAPIUrl(countryCode.toUpperCase());
						exchange.setProperty("countryCode", countryCode);
					}
					exchange.setProperty("hostURL", hostURL);
					exchange.setProperty("getAccessToken", true);
				} else {
					log.error("postHelper not found for this account: " + exchange.getProperty("accountNumber")
							+ " nickNameId: " + id);
				}
				if (userChannel.has("sellerOwnFleet")) {
					JSONObject sellerOwnFleet = userChannel.getJSONObject("sellerOwnFleet");
					if (sellerOwnFleet.has("username") && sellerOwnFleet.has("password")) {
						exchange.setProperty("sofUsername", sellerOwnFleet.getString("username"));
						exchange.setProperty("sofPassword",
								EncryptionUtil.decrypt(sellerOwnFleet.getString("password")));
						exchange.setProperty("getSOFToken", true);
					}
				}
				break;
			}
		}
	}
}
