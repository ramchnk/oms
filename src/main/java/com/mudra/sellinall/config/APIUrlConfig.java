package com.mudra.sellinall.config;

import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class APIUrlConfig {
	public static ApplicationContext context;

	private String LazadaAPIForSG;
	private String LazadaAPIForMY;
	private String LazadaAPIForID;
	private String LazadaAPIForPH;
	private String LazadaAPIForTH;
	private String LazadaAPIForVN;
	private String NewLazadaAPIForSG;
	private String NewLazadaAPIForMY;
	private String NewLazadaAPIForID;
	private String NewLazadaAPIForPH;
	private String NewLazadaAPIForTH;
	private String NewLazadaAPIForVN;

	public static APIUrlConfig getConfig() {
		return (APIUrlConfig) context.getBean("APIUrl");
	}

	public static String getAPIUrl(String countryCode) {
		if (countryCode.equals("SG")) {
			return APIUrlConfig.getConfig().getLazadaAPIForSG();
		} else if (countryCode.equals("MY")) {
			return APIUrlConfig.getConfig().getLazadaAPIForMY();
		} else if (countryCode.equals("ID")) {
			return APIUrlConfig.getConfig().getLazadaAPIForID();
		} else if (countryCode.equals("PH")) {
			return APIUrlConfig.getConfig().getLazadaAPIForPH();
		} else if (countryCode.equals("TH")) {
			return APIUrlConfig.getConfig().getLazadaAPIForTH();
		} else if (countryCode.equals("VN")) {
			return APIUrlConfig.getConfig().getLazadaAPIForVN();
		}
		return "";
	}

	public static String getNewAPIUrl(String countryCode) {
		if (countryCode.equals("SG")) {
			return APIUrlConfig.getConfig().getNewLazadaAPIForSG();
		} else if (countryCode.equals("MY")) {
			return APIUrlConfig.getConfig().getNewLazadaAPIForMY();
		} else if (countryCode.equals("ID")) {
			return APIUrlConfig.getConfig().getNewLazadaAPIForID();
		} else if (countryCode.equals("PH")) {
			return APIUrlConfig.getConfig().getNewLazadaAPIForPH();
		} else if (countryCode.equals("TH")) {
			return APIUrlConfig.getConfig().getNewLazadaAPIForTH();
		} else if (countryCode.equals("VN")) {
			return APIUrlConfig.getConfig().getNewLazadaAPIForVN();
		}
		return "";
	}
}
