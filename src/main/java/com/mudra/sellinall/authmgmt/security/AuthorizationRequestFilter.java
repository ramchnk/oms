package com.mudra.sellinall.authmgmt.security;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import com.mudra.sellinall.config.Config;
import com.sellinall.util.AuthConstant;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

public class AuthorizationRequestFilter implements ContainerRequestFilter {
	static Logger log = Logger.getLogger(AuthorizationRequestFilter.class
			.getName());

	public ContainerRequest filter(ContainerRequest arg0)
			throws WebApplicationException {
		arg0.getProperties().put("apiExecutionStartTime", System.currentTimeMillis());
		if (arg0.getMethod().equals("OPTIONS")) {
			throw new WebApplicationException(Status.ACCEPTED);
		}
		
		if (arg0.getPath().contains("settings/complete")) {
			log.debug("NO Need To authorize this url because it is from lazada / complete account service");
			return arg0;
		}
		if(arg0.getPath().contains("health")) {
			log.debug("No Need To authorize this url because it is from LAZADALISTING / health service ");
			return arg0;
		}
		if(arg0.getPath().contains("login")) {
			log.debug("No Need To authorize this url because it is from LAZADALISTING / health service ");
			return arg0;
		}
		
		if(arg0.getPath().contains("settings/initiate")) {
			log.debug("No Need To authorize this url because it is from LAZADALISTING / health service ");
			return arg0;
		}
		
		if(arg0.getPath().contains("orders/lazada")) {
			log.debug("No Need To authorize this url because it is from LAZADALISTING / health service ");
			return arg0;
		}
		if(arg0.getPath().contains("orderitems/lazada")) {
			log.debug("No Need To authorize this url because it is from LAZADALISTING / health service ");
			return arg0;
		}
		if(arg0.getPath().contains("documents/lazada")) {
			log.debug("No Need To authorize this url because it is from LAZADALISTING / health service ");
			return arg0;
		}
		/*
		 * if (arg0.getAbsolutePath().getHost().equals("localhost")) { return
		 * arg0; }
		 */

		log.debug("method is " + arg0.getMethod());
		try {
			if (arg0.getHeaderValue(AuthConstant.RAGASIYAM_KEY) != null && Config.getConfig().getRagasiyam() != null
					&& checkValidUser(arg0.getHeaderValue(AuthConstant.RAGASIYAM_KEY).split(","),
							Config.getConfig().getRagasiyam().split(","))) {
				InBoundHeaders headers = new InBoundHeaders();
				String accountNumber = arg0.getHeaderValue("accountNumber");
				headers.add("accountNumber", accountNumber);
				// we can pass multiple ragasiyam values using comma separator
				headers.add(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
				headers.add("Content-Type", "application/json");
				arg0.setHeaders(headers);
				return arg0;
			}
		} catch (Exception e) {
			log.error(arg0.getAbsolutePath().getHost());
			log.error("some Exception or some one hacking\n" + e);
		}
		// break the code since it reached here
		log.debug("UNAUTHORIZED");
		throw new WebApplicationException(Status.UNAUTHORIZED);
	}

	public boolean checkValidUser(String ragasiyam[], String originalvalue[]) {
		boolean flag = false;
		for (int i = 0; i < ragasiyam.length; i++) {
			for (int j = 0; j < originalvalue.length; j++) {
				if (ragasiyam[i] != null && originalvalue[j] != null && ragasiyam[i].equals(originalvalue[j])) {
					flag = true;
					break;
				}
			}
		}
		return flag;
	}
}
