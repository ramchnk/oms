	/**
 * 
 */
package com.sellinall.lazada.init;

import java.util.HashSet;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;

/**
 * @author vikraman
 *
 */
public class InitializePullInventoryRoute implements Processor {
	static Logger log = Logger.getLogger(InitializePullInventoryRoute.class.getName());

	public void process(Exchange exchange) throws Exception {

		JSONObject input = exchange.getIn().getBody(JSONObject.class);
		log.debug("accountNumber: " + input.getString("accountNumber"));
		if(!input.has("retryCount")) {
			input.put("retryCount", 0);
		}
		exchange.setProperty("initialRetryMessage", input);
		exchange.setProperty("accountNumber", input.getString("accountNumber"));
		String nickNameID = input.getString("nickNameId");
		exchange.setProperty("nickNameID", nickNameID);
		exchange.setProperty("channelName", nickNameID.split("-")[0]);
		exchange.setProperty("importRecordObjectId", input.getString("importRecordObjectId"));
		if(input.has("importType")) {
			exchange.setProperty("importType", input.getString("importType"));
		}
		if(input.has("fromDate")) {
			exchange.setProperty("fromDate", input.getLong("fromDate"));
		}
		if(input.has("toDate")) {
			exchange.setProperty("toDate", input.getLong("toDate"));
		}
		if(input.has("countryCode")) {
			exchange.setProperty("importCountry", input.getString("countryCode"));
		}
		if (input.has("importType") && input.getString("importType").equals("noOfRecords")) {
			int numberOfRecords = input.getInt("numberOfRecords");
			exchange.setProperty("numberOfRecords", numberOfRecords);
			int recordsPerPage = Config.getConfig().getRecordsPerPage();
			int numberOfPages = (numberOfRecords / recordsPerPage) + ((numberOfRecords % recordsPerPage > 0) ? 1 : 0);
			exchange.setProperty("numberOfPages", numberOfPages);
			log.debug("No Of Record = " + numberOfRecords);
		}
		if(input.has("importStatusFilter")) {
			exchange.setProperty("importStatusFilter", input.getString("importStatusFilter"));
		}
		exchange.setProperty("noOfItemCompleted", 0);
		exchange.setProperty("noOfItemLinked", 0);
		exchange.setProperty("noOfItemUnLinked", 0);
		exchange.setProperty("noOfItemSkipped", 0);
		exchange.setProperty("pageNumber", 1);
		exchange.setProperty("totalEntries", 0);
		exchange.setProperty("offset", 0);
		exchange.setProperty("isLastLoop", false);
		Set<String> itemIDList = new HashSet<String>();
		exchange.setProperty("itemIDList", itemIDList);
		if (input.has("userId")) {
			exchange.setProperty("userId", input.getString("userId"));
		}
		if (input.has("userName")) {
			exchange.setProperty("userName", input.getString("userName"));
		}
	}
}