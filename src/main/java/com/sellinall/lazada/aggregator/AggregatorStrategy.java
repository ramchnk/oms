/**
 * 
 */
package com.sellinall.lazada.aggregator;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Senthil
 *
 */
public class AggregatorStrategy implements AggregationStrategy {

	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		if (oldExchange == null) {
			JSONArray orderItems = new JSONArray();
			orderItems.put(newExchange.getIn().getBody(JSONObject.class));
			newExchange.getIn().setBody(orderItems);
			return newExchange;
		}

		JSONArray orderItems = oldExchange.getIn().getBody(JSONArray.class);
		orderItems.put(newExchange.getIn().getBody(JSONObject.class));
		return oldExchange;
	}
}