package com.sellinall.lazada.process;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mudra.sellinall.config.Config;

public class ValidateSettlementSheet implements Processor {

	public void process(Exchange exchange) throws Exception {
		List<List<String>> orderList = exchange.getProperty("orderList", ArrayList.class);
		Set<String> errorSet = new HashSet<String>();
		boolean isValidDateFormat = isValidateSettlementDate(orderList.get(1).get(0));
		for (int i = 1; i < orderList.size(); i++) {
			List<String> row = orderList.get(i);
			if (!isValidateFeesName(row.get(2))) {
				errorSet.add(row.get(2));
			}
		}
		String failureReason = "";
		String warning = "";
		if (!isValidDateFormat) {
			failureReason = "Date format should be (dd MMM yyyy),(for example : 02 Mar 2020)";
		}
		if (!errorSet.isEmpty()) {
			warning += " Unexpected fields found -";
			warning += String.join(", ", errorSet);
			if (!failureReason.isEmpty()) {
				failureReason = failureReason + " , and" + warning;
			}
		}
		if (!failureReason.isEmpty()) {
			failureReason = "Invalid Settlement Sheet." + failureReason;
		}
		exchange.setProperty("failureReason", failureReason);
		if (!warning.isEmpty()) {
			exchange.setProperty("warning", warning);
		}
	}

	private boolean isValidateFeesName(String transactionType) {
		return Config.getConfig().getSettlementSheetHeader().contains(transactionType);
	}

	private boolean isValidateSettlementDate(String date) {
		DateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
		try {
			df.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
