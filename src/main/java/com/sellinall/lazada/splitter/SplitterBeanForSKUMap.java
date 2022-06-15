/**
 * 
 */
package com.sellinall.lazada.splitter;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Body;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultMessage;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author malli
 * 
 */
public class SplitterBeanForSKUMap {
	static Logger log = Logger.getLogger(SplitterBeanForSKUMap.class.getName());

	public List<Message> splitSKUMap(@Body JSONObject body) throws JSONException {
		// we can leverage the Parameter Binding Annotations
		// http://camel.apache.org/parameter-binding-annotations.html
		// to access the message header and body at same time,
		// then create the message that we want, splitter will
		// take care rest of them.
		// *NOTE* this feature requires Camel version >= 1.6.1
		JSONArray SKUMaps = (JSONArray) body.get("SKUMap");
		List<Message> answer = new ArrayList<Message>();
		for (int index = 0; index < SKUMaps.length(); index++) {
			JSONObject SKUMap = SKUMaps.getJSONObject(index);
			SKUMap.put("siteNicknames", body.get("siteNicknames"));
			DefaultMessage message = new DefaultMessage();
			message.setBody(SKUMap);
			answer.add(message);
		}
		return answer;
	}
}