/**
 * 
 */
package com.sellinall.lazada.aggregator;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;

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
public class PullInventoryAggregator implements AggregationStrategy {
	static Logger log = Logger.getLogger(PullInventoryAggregator.class.getName());

	@SuppressWarnings("unchecked")
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		try {
			List<BasicDBObject> newUnlinkInventoryList = newExchange.getIn().getBody(List.class);
			if (oldExchange == null) {
				newExchange.getOut().setBody(newUnlinkInventoryList);
				return newExchange;
			}

			if (oldExchange.getIn().getBody() == null ) {
				newExchange.getOut().setBody(newUnlinkInventoryList);
				return newExchange;
			}

			List<BasicDBObject> oldUnlinkInventoryList = (ArrayList<BasicDBObject>) oldExchange.getIn().getBody();
			oldUnlinkInventoryList.addAll(newUnlinkInventoryList);
			newExchange.getOut().setBody(oldUnlinkInventoryList);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return newExchange;
	}
}