package com.mudra.sellinall.database;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.mongodb.DB;
import com.mongodb.DBCollection;

public class DbUtilities {

	static ApplicationContext userContext = new AnnotationConfigApplicationContext(UserAndAuthCfg.class);
	static DB userDB = (DB) userContext.getBean("db");

	public static DBCollection getDBCollection(String collectionName) {
		return userDB.getCollection(collectionName);
	}

	
	static ApplicationContext inventoryContext = new AnnotationConfigApplicationContext(InventoryCfg.class);
	static DB inventoryDB = (DB) inventoryContext.getBean("db");
	static DB inventoryRODB = (DB) inventoryContext.getBean("dbRO");
	static ApplicationContext orderContext = new AnnotationConfigApplicationContext(OrderCfg.class);
	static DB orderDB = (DB) orderContext.getBean("db");
	static ApplicationContext ctxNotification = new AnnotationConfigApplicationContext(NotificationCfg.class);
	static DB dbNotification = (DB) ctxNotification.getBean("db");

	public static DBCollection getInventoryDBCollection(String collectionName) {
		return inventoryDB.getCollection(collectionName);
	}
	public static DBCollection getROInventoryDBCollection(String collectionName) {
		return inventoryRODB.getCollection(collectionName);
	}
	public static DBCollection getOrderDBCollection(String collectionName) {
		return orderDB.getCollection(collectionName);
	}
	public static DBCollection getDBNotificationCollection(String collectionName) {
		return dbNotification.getCollection(collectionName);
	}

}
