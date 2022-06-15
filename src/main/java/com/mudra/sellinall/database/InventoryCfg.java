package com.mudra.sellinall.database;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import com.mudra.sellinall.config.Config;

@Configuration
public class InventoryCfg {	
	
	public @Bean DB db() throws Exception {
		MongoClientURI uri = new MongoClientURI(Config.getConfig().getInventoryConfigDBURI());
		MongoClient mongoClient = new MongoClient(uri);
		DB db = mongoClient.getDB(Config.getConfig().getInventoryConfigDBName());
		return db;
	}
	public @Bean DB dbRO() throws Exception {
		ReadPreference readPreference = ReadPreference.secondaryPreferred();
		MongoClientOptions.Builder builder =  MongoClientOptions.builder();
		builder.readPreference(readPreference);
		MongoClientURI uri = new MongoClientURI(Config.getConfig().getInventoryConfigDBURI(), builder);
		MongoClient mongoClient = new MongoClient(uri);
		DB db = mongoClient.getDB(Config.getConfig().getInventoryConfigDBName());
		return db;
	
	}
}
