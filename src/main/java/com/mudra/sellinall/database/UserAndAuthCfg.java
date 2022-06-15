package com.mudra.sellinall.database;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mudra.sellinall.config.Config;

@Configuration
public class UserAndAuthCfg {
	public @Bean DB db() throws Exception {
		MongoClientURI uri = new MongoClientURI(Config.getConfig().getUserConfigDBURI());
		MongoClient mongoClient = new MongoClient(uri);
		DB db = mongoClient.getDB(Config.getConfig().getUserConfigDBName());
		return db;
	}
}