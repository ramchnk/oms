package com.sellinall.lazada.process;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class CheckGetShippingLabel implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		boolean isAutoPackOrder = false, isRepackOrder = false, isLazadaIntegratedShippingCarrier = false;
		if (exchange.getProperties().containsKey("isAutoPackOrder")) {
			isAutoPackOrder = exchange.getProperty("isAutoPackOrder", Boolean.class);
		}
		if (exchange.getProperties().containsKey("isRepackOrder")) {
			isRepackOrder = exchange.getProperty("isRepackOrder", Boolean.class);
		}
		if (exchange.getProperties().containsKey("isLazadaIntegratedShippingCarrier")) {
			isLazadaIntegratedShippingCarrier = exchange.getProperty("isLazadaIntegratedShippingCarrier",
					Boolean.class);
		}
		boolean isEligibleToGetShippingLabel = false;
		if (exchange.getProperties().containsKey("updateStatus")
				&& exchange.getProperty("updateStatus", String.class).equals("COMPLETE")
				&& (isAutoPackOrder || isRepackOrder || isLazadaIntegratedShippingCarrier)) {
			isEligibleToGetShippingLabel = true;
		}
		exchange.setProperty("isEligibleToGetShippingLabel", isEligibleToGetShippingLabel);
	}

}
