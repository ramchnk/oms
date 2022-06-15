package com.sellinall.lazada.requests;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.util.NewLazadaConnectionUtil;

public class GetListOfFlexiComboProducts implements Processor {
	static Logger log = Logger.getLogger(GetListOfFlexiComboProducts.class.getName());
	public boolean hasMoreProduct;
	public int productListCurrentPage;
	public int productListPageSize;

	public void process(Exchange exchange) throws Exception {
		JSONObject dataListObject = exchange.getIn().getBody(JSONObject.class);
		// Re-assigning the variables for each thread
		hasMoreProduct = true;
		productListCurrentPage = 1;
		productListPageSize = 100;
		JSONArray productsList = new JSONArray();
		if (dataListObject.has("apply") && dataListObject.getString("apply").equalsIgnoreCase("SPECIFIC_PRODUCTS")) {
			while (hasMoreProduct) {
				if (dataListObject.has("id")) {
					JSONArray productList = getFlexiComboProducts(exchange, dataListObject.getString("id"));
					mergeSKUIDList(productsList, productList);
				}
			}
			dataListObject.put("products", productsList);
		}
		exchange.setProperty("fexiComboItemWithProduct", dataListObject);
	}

	public static void mergeSKUIDList(JSONArray target, JSONArray source) throws JSONException {
		for (int i = 0; i < source.length(); i++) {
			target.put(source.getString(i));
		}
	}

	public JSONArray getFlexiComboProducts(Exchange exchange, String flexiComboId) {
		JSONArray responseDataList = new JSONArray();
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostURL = exchange.getProperty("hostURL", String.class);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("cur_page", String.valueOf(productListCurrentPage));
		map.put("page_size", String.valueOf(productListPageSize));
		map.put("id", flexiComboId);
		map.put("access_token", accessToken);
		String queryParams = "";
		queryParams += "&cur_page=" + productListCurrentPage;
		queryParams += "&page_size=" + productListPageSize;
		queryParams += "&id=" + flexiComboId;
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		String response = "";
		try {
			response = NewLazadaConnectionUtil.callAPI(hostURL, "/promotion/flexicombo/products/list", accessToken, map,
					"", queryParams, "GET", clientID, clientSecret);
			if (response.isEmpty()) {
				hasMoreProduct = false;
				log.error("Failed to get list of flexi combo products for accountNumber :"
						+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
						+ exchange.getProperty("nickNameID", String.class) + " and response is" + response.toString());
				exchange.setProperty("failureReason", "Getting empty response from lazada, please try again");
				return responseDataList;
			}
			JSONObject serviceResponse = new JSONObject(response);
			if (serviceResponse.has("code") && serviceResponse.getString("code").equals("0")) {
				if (!serviceResponse.has("data")) {
					return responseDataList;
				}
				JSONObject responseData = serviceResponse.getJSONObject("data");
				if (responseData.has("data_list")) {
					responseDataList = responseData.getJSONArray("data_list");
					controlGetListProductPagination(serviceResponse.getJSONObject("data"));
					return responseDataList;
				}
				hasMoreProduct = false;
				return responseDataList;
			}
			hasMoreProduct = false;
			log.error("Failed to get list of flexi combo products for accountNumber :"
					+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response is" + response.toString());
			exchange.setProperty("failureReason", "unable to import" + response.toString());
			if (serviceResponse.has("message")) {
				exchange.setProperty("failureReason", serviceResponse.getString("message"));
			}
		} catch (Exception e) {
			hasMoreProduct = false;
			log.error("Exception ocurred while get list of flexi combo products  for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " and nickNameID : "
					+ exchange.getProperty("nickNameID", String.class) + " and response is " + response.toString(), e);
			exchange.setProperty("failureReason", "Unable to import" + e.getMessage());
		}
		return responseDataList;
	}

	public void controlGetListProductPagination(JSONObject data) throws NumberFormatException, JSONException {
		int total = Integer.parseInt(data.getString("total"));
		int pageSize = Integer.parseInt(data.getString("page_size"));
		if (total <= pageSize) {
			hasMoreProduct = false;
			return;
		}
		double totalpageNumber = Math.ceil(total * 1.0 / pageSize * 1.0);
		if (totalpageNumber <= (productListCurrentPage * 1.0)) {
			hasMoreProduct = false;
		}
		productListCurrentPage += 1;
	}
}
