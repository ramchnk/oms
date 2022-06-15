package com.sellinall.lazada.init;


import java.util.Calendar;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

public class CheckReconcileTimeRange implements Processor {
	static Logger log = Logger.getLogger(CheckReconcileTimeRange.class.getName());
	
	public void process(Exchange exchange) throws Exception {
		exchange.setProperty("isDateWithinReconcileLimit", checkDateAndTimeRange());		
	}
	
	private boolean checkDateAndTimeRange(){
		//Now we are having only 2 cycles
		Calendar cal = Calendar.getInstance();
		int currentDayInMonth = cal.get(Calendar.DAY_OF_MONTH);
		int secondCycleDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH) - 1;
		if ((currentDayInMonth == 14 || currentDayInMonth == secondCycleDay) && cal.get(Calendar.HOUR_OF_DAY) == 12
				&& cal.get(Calendar.MINUTE) < 6) {
			return true;
		}
		return false;
	}
}
