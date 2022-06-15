package com.sellinall.lazada.init;

import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mongodb.DBObject;
import com.mudra.sellinall.config.APIUrlConfig;

public class InitCountryBasedInventories implements Processor {

	public void process(Exchange exchange) throws Exception {
		String countryCode = exchange.getIn().getBody(String.class);
		Map<String, List<DBObject>> countryBasedInventoryMap = exchange.getProperty("countryBasedInventoryMap",
				Map.class);
		List<DBObject> countryBaseInventoryList = countryBasedInventoryMap.get(countryCode);

		exchange.setProperty("inventoryList", countryBaseInventoryList);
		exchange.setProperty("countryCode", countryCode);
		exchange.setProperty("hostURL", APIUrlConfig.getNewAPIUrl(countryCode));
		exchange.setProperty("isLastPage", false);
	}

}
