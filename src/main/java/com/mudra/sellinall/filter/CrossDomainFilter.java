package com.mudra.sellinall.filter;

import com.mudra.sellinall.filter.CrossDomainFilter;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.apache.log4j.Logger;

/**
 *  * Allow the system to serve xhr level 2 from all cross domain site  *  * @author
 * Deisss (LGPLv3)  * @version 0.1  
 */
public class CrossDomainFilter implements ContainerResponseFilter {
	static Logger log = Logger.getLogger(CrossDomainFilter.class.getName());

	/**
	 *      * Add the cross domain data to the output if needed      *      * @param
	 * creq The container request (input)      * @param cres The container
	 * request (output)      * @return The output request with cross domain if
	 * needed      
	 */
	public ContainerResponse filter(ContainerRequest creq,ContainerResponse cres) {
		System.out.println("Inside Cross Domain same origin Response FIlter ");
		cres.getHttpHeaders().add("Access-Control-Allow-Origin", "*");
		cres.getHttpHeaders().add("Access-Control-Allow-Headers",
				"origin, content-type, accept, authorization, Mudra, username");
		cres.getHttpHeaders().add("Access-Control-Allow-Credentials", "true");
		cres.getHttpHeaders().add("Access-Control-Allow-Methods",
				"GET, POST, PUT, DELETE");
		cres.getHttpHeaders().add("Access-Control-Max-Age", "1209600");
		cres.getHttpHeaders().add("Content-type", "text/html");
		/* Note: this change required only for GCP servers */
		logRequestedServiceCall(creq, cres);
		return cres;
	}
	private void logRequestedServiceCall(ContainerRequest creq, ContainerResponse cres) {
		String logMessage = "method=" + creq.getMethod();
		logMessage += " path=" + creq.getAbsolutePath().getRawPath();
		if (creq.getRequestUri().getRawQuery() != null) {
			logMessage += "?" + creq.getRequestUri().getRawQuery();
		}
		logMessage += " host=" + creq.getRequestUri().getHost();
		logMessage += " service="+ getProcessedTime(Long.parseLong(creq.getProperties().get("apiExecutionStartTime").toString())) + "ms";
		logMessage += " status=" + cres.getStatus();
		log.info(logMessage);
	}

	private String getProcessedTime(long startTime) {
		long endTime = System.currentTimeMillis();
		endTime = endTime - startTime;
		return String.valueOf(endTime);
	}
}