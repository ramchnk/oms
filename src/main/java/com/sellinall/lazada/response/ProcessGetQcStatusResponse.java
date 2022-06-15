package com.sellinall.lazada.response;

import java.util.ArrayList;

import net.spy.memcached.MemcachedClient;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.lazada.db.UpdateSKUToMemoryCache;
import com.sellinall.lazada.util.LazadaUtil;

/**
 * @author Ramachandran.k
 * 
 */
public class ProcessGetQcStatusResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessGetQcStatusResponse.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject qcResponse = exchange.getIn().getBody(JSONObject.class);
		log.debug("QC Response Details " + qcResponse.toString());
		ArrayList<JSONObject> list = new ArrayList<JSONObject>();
		if (!qcResponse.has("SuccessResponse")) {
			exchange.getOut().setBody(list);
			return;
		}
		JSONObject status = qcResponse.getJSONObject("SuccessResponse").getJSONObject("Body").getJSONObject("Status");
		JSONArray state = status.getJSONArray("State");
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String channelName = exchange.getProperty("channelName", String.class);
		String nickNameId = exchange.getProperty("nickNameID", String.class);
		String MCKey = LazadaUtil.getMCkeyforGcStatusWaitingSKUS(channelName, accountNumber, nickNameId);
		MemcachedClient mc = LazadaUtil.getMemcachedClient();
		ArrayList<String> SKUList = (ArrayList<String>) LazadaUtil.getValueFromMemcache(MCKey, true);
		for (int i = 0; i < state.length(); i++) {
			JSONObject qcStatus = state.getJSONObject(i);
			String sellerSKU = qcStatus.getString("SellerSKU");
			if (SKUList.contains(sellerSKU)) {
				list.add(qcStatus);
				SKUList.remove(SKUList.indexOf(sellerSKU));
			}
		}
		mc.set(MCKey, UpdateSKUToMemoryCache.MC_MAX_EXPIRE_TIME, SKUList);
		exchange.getOut().setBody(list);
	}
}