package com.sellinall.lazada;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.log4j.BasicConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.mudra.sellinall.config.APIUrlConfig;
import com.mudra.sellinall.config.Config;
import com.sellinall.lazada.services.AccountService;
import com.sellinall.lazada.services.BrandService;
import com.sellinall.lazada.services.CategoriesService;
import com.sellinall.lazada.services.CategoryAttributeService;
import com.sellinall.lazada.services.CategorySuggestionService;
import com.sellinall.lazada.services.ChannelDataService;
import com.sellinall.lazada.services.DescriptionServices;
import com.sellinall.lazada.services.GetAPIDetailService;
import com.sellinall.lazada.services.GetDocumentServices;
import com.sellinall.lazada.services.ItemPrice;
import com.sellinall.lazada.services.Listing;
import com.sellinall.lazada.services.PolicyServices;
import com.sellinall.lazada.services.ChatService;
import com.sellinall.lazada.util.LazadaUtil;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.EncryptionUtil;
import com.sellinall.util.InventorySequence;
import com.sellinall.util.ListingStockEventSequence;

/**
 * 
 * This class launches the web application in an embedded Jetty container. This
 * is the entry point to your application. The Java command that is used for
 * launching should fire this main method.
 * 
 */
public class MainPrg {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String webappDirLocation = "src/main/webapp/";
		Config.context = new ClassPathXmlApplicationContext("Propertycfg.xml");
		APIUrlConfig.context = new ClassPathXmlApplicationContext("APIUrlcfg.xml");
		// init db sequence
		Config config = Config.getConfig();
		InventorySequence.init(config.getInventoryConfigDBURI(), config.getInventoryConfigDBName());
		config.setRagasiyam(System.getenv(AuthConstant.RAGASIYAM_KEY));
		ListingStockEventSequence.init(config.getInventoryConfigDBURI(), config.getInventoryConfigDBName());

		// The port that we should run on can be set into an environment
		// variable
		// Look for that variable and default to 8081 if it isn't there.
		String webPort = System.getenv("PORT");
		if (webPort == null || webPort.isEmpty()) {
			webPort = "8081";
		}

		Server server = new Server(Integer.valueOf(webPort));
		WebAppContext root = new WebAppContext();

		root.setContextPath("/");
		root.setDescriptor(webappDirLocation + "/WEB-INF/web.xml");
		root.setResourceBase(webappDirLocation);
		BasicConfigurator.configure();
		// Parent loader priority is a class loader setting that Jetty accepts.
		// By default Jetty will behave like most web containers in that it will
		// allow your application to replace non-server libraries that are part
		// of the
		// container. Setting parent loader priority to true changes this
		// behavior.
		// Read more here:
		// http://wiki.eclipse.org/Jetty/Reference/Jetty_Classloading
		root.setParentLoaderPriority(true);

		server.setHandler(root);
		EncryptionUtil.init();
		// Init memory cache
		LazadaUtil.initMemoryCached();
		ApplicationContext appContext = new ClassPathXmlApplicationContext("CamelContext.xml");
		CamelContext camelContext = SpringCamelContext.springCamelContext(appContext, false);
		camelContext.start();

		ProducerTemplate template=camelContext.createProducerTemplate();
		Listing.setProducerTemplate(template);
		AccountService.setProducerTemplate(template);
		PolicyServices.setProducerTemplate(template);
		GetDocumentServices.setProducerTemplate(template);
		DescriptionServices.setProducerTemplate(template);
		CategoriesService.setProducerTemplate(template);
		CategoryAttributeService.setProducerTemplate(template);
		ChatService.setProducerTemplate(template);
		CategorySuggestionService.setProducerTemplate(template);
		ItemPrice.setProducerTemplate(template);
		BrandService.setProducerTemplate(template);
		GetAPIDetailService.setProducerTemplate(template);
		ChannelDataService.setProducerTemplate(template);
		server.start();
		server.join();
	}

}
