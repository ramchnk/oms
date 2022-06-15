package com.sellinall.lazada.response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.sellinall.util.HttpsURLConnectionUtil;

public class ProcessItemDescription implements Processor {
	static Logger log = Logger.getLogger(ProcessItemDescription.class.getName());

	public void process(Exchange exchange) throws Exception {
		String page = "";
		JSONObject response = null;
		try {
			response = HttpsURLConnectionUtil.doGet(exchange.getProperty("itemUrl", String.class), null);
			if (response.has("payload")) {
				page = response.getString("payload");
			}
		} catch (Exception e) {
			log.error("unable to fetch item url " + exchange.getProperty("itemUrl", String.class) + " for nickNameId:"
					+ exchange.getProperty("nickNameID", String.class) + " for accountNumber: "
					+ exchange.getProperty("accountNumber", String.class) + " and response: " + response.toString());
			e.printStackTrace();
		}
		if (page.isEmpty()) {
			exchange.getOut().setBody(null);
			return;
		}
		Document html = Jsoup.parse(page);
		Object[] descriptionBlock = html.getElementsByClass("product-description__block").toArray();
		if (descriptionBlock.length < 1) {
			// Item is not active
			exchange.getOut().setBody(null);
			return;
		}
		// in lazada page we have more than one description div
		// we need first div block only
		String itemDescription = descriptionBlock[0].toString();
		exchange.getOut().setBody(itemDescription.replace("data-original=\"", "src=\""));
	}

}