/**
 * 
 */
package com.sellinall.lazada.splitter;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;

/**
 * @author malli
 *
 */
/**
 * This is our own order aggregation strategy where we can control how each
 * splitted message should be combined. As we do not want to loos any message we
 * copy from the new to the old to preserve the order lines as long we process
 * them
 */
public class SKUMapAggregator implements AggregationStrategy {
	static Logger log = Logger.getLogger(SKUMapAggregator.class.getName());

	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		try {
			JSONArray newSKUMapList = newExchange.getIn().getBody(JSONArray.class);
			if (oldExchange == null) {
				newExchange.getOut().setBody(newSKUMapList);
				return newExchange;
			}
			JSONArray oldSKUMapList = oldExchange.getIn().getBody(JSONArray.class);
			oldSKUMapList.put(newSKUMapList);
			newExchange.getOut().setBody(oldSKUMapList);
			log.debug("aggregated SKUMap :"+oldSKUMapList);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return newExchange;
	}
}