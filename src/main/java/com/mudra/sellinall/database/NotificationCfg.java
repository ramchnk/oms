package com.mudra.sellinall.database;

import org.springframework.context.annotation.Bean;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mudra.sellinall.config.Config;

public class NotificationCfg {

	public @Bean DB db() throws Exception {
		MongoClientURI uri = new MongoClientURI(Config.getConfig().getNotificationDBURI());
		MongoClient mongoClient = new MongoClient(uri);
		DB db = mongoClient.getDB(Config.getConfig().getNotificationDBName());
		return db;
	}
}
