package com.mudra.sellinall.config;

import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Config {
	public static ApplicationContext context;

	private String FbTokenExchangeUri;

	private String userConfigDBURI;
	private String userConfigDBName;
	private String inventoryConfigDBURI;
	private String inventoryConfigDBName;
	private String notificationDBURI;
	private String notificationDBName;
	private String uploadImageToSellInAllUrl;
	private String uploadImageUri;
	private String noImageURL;
	private String noImageUploadImageToSellInAllUrl;
	private String validateImageServer;
	private String SIAListingLookupServerURL;
	private String SIAPromotionServerURL;
	private String SIACategoryNameLookupURL;
	private String SettingsCompletedURL;
	private String chatReauthCompletedURLAdmin;

	private String CurrencyForSG;
	private String CurrencyForID;
	private String CurrencyForMY;
	private String CurrencyForPH;
	private String CurrencyForTH;
	private String CurrencyForVN;

	private int RecordsPerPage;
	private boolean needToPublishItemSyncMessage;

	private String MemcachedCloudUsername;
	private String MemcachedCloudPassword;
	private String MemcachedCloudServers;
	private String Ragasiyam;
	private String completeAuthUrl;
	private String initiateAuthURL;

	private String LazadaClientID;
	private String LazadaChatClientID;
	private String LazadaChatClientSecret;
	private String LazadaClientSecret;
	private String selfEndpoint;

	private String orderLimit;
	private String siaIntegratedShippingProviderList;

	private String orderConfigDBURI;
	private String orderConfigDBName;

	private String partnerSignupChannels;
	private String partnerSignUpCompletedUrl;
	private String sofPortalURLSG;
	private String sofPortalURLMY;
	private String sofPortalURLID;
	private String lazadaCommissionFeePercent;
	private String sofEnabledCountries;
	private String lazadaAppName;
	private String lazadaChatAppName;
	private String commonApiUrl;

	private String commonRefreshTokenSG;
	private String commonRefreshTokenID;
	private String commonRefreshTokenMY;
	private String commonRefreshTokenPH;
	private String commonRefreshTokenTH;

	private String commonClientID;
	private String commonClientSecret;
	private String lazadaIntegratedShippingCarriers;
	private String uploadShippinglabelToSELLinALLUrl;
	private String uploadShippinglabelUrl;
	private boolean individualSKUPerChannelEnabled;
	private boolean updateStockViaProductUpdateApi;
	private boolean updateStockViaSellableQuantityApi;
	private String settlementSheetHeader;

	private String inventoryUrl;
	private String SIAOrderURL;
	private int defaultSettlementDateRangeSG;
	private int defaultSettlementDateRangeID;
	private int defaultSettlementDateRangeMY;
	private int defaultSettlementDateRangePH;
	private int defaultSettlementDateRangeTH;
	private int defaultSettlementDateRangeVN;

	private String orderFeeNames;
	private String refundFeeNames;
	private String claimFeeNames;

	private String testAccountNumber;
	private String globalCountryCode;

	private String removeKeyPropFieldTitleList;
	private String promotionStartDate;
	private String promotionEndDate;


	public String getSIAOrderURL() {
		return SIAOrderURL;
	}

	public void setSIAOrderURL(String sIAOrderURL) {
		SIAOrderURL = sIAOrderURL;
	}

	public static Config getConfig() {
		return (Config) context.getBean("Config");
	}

	public String getChatReauthCompletedURLAdmin() {
		return chatReauthCompletedURLAdmin;
	}

	public void setChatReauthCompletedURLAdmin(String chatReauthCompletedURLAdmin) {
		this.chatReauthCompletedURLAdmin = chatReauthCompletedURLAdmin;
	}

	public String getGlobalCountryCode() {
		return globalCountryCode;
	}

	public void setGlobalCountryCode(String globalCountryCode) {
		this.globalCountryCode = globalCountryCode;
	}

	public String getCurrencyCode(String countryCode) {
		if (countryCode.equals("SG")) {
			return getCurrencyForSG();
		} else if (countryCode.equals("MY")) {
			return getCurrencyForMY();
		} else if (countryCode.equals("ID")) {
			return getCurrencyForID();
		} else if (countryCode.equals("PH")) {
			return getCurrencyForPH();
		} else if (countryCode.equals("TH")) {
			return getCurrencyForTH();
		} else if (countryCode.equals("VN")) {
			return getCurrencyForVN();
		}
		return "";
	}

	public String getCommonRefreshToken(String countryCode) {
		if (countryCode.equals("SG")) {
			return getCommonRefreshTokenSG();
		} else if (countryCode.equals("MY")) {
			return getCommonRefreshTokenMY();
		} else if (countryCode.equals("ID")) {
			return getCommonRefreshTokenID();
		} else if (countryCode.equals("PH")) {
			return getCommonRefreshTokenPH();
		} else if (countryCode.equals("TH")) {
			return getCommonRefreshTokenTH();
		}
		return "";
	}

	public String getSOFPortalUrl(String countryCode) {
		if (countryCode.equals("SG")) {
			return getSofPortalURLSG();
		} else if (countryCode.equals("MY")) {
			return getSofPortalURLMY();
		} else if (countryCode.equals("ID")) {
			return getSofPortalURLID();
		}
		return "";
	}

	public int getDefaultSettlementDateRange(String countryCode) {
		if (countryCode.equals("SG")) {
			return getDefaultSettlementDateRangeSG();
		} else if (countryCode.equals("MY")) {
			return getDefaultSettlementDateRangeMY();
		} else if (countryCode.equals("ID")) {
			return getDefaultSettlementDateRangeID();
		} else if (countryCode.equals("PH")) {
			return getDefaultSettlementDateRangePH();
		} else if (countryCode.equals("TH")) {
			return getDefaultSettlementDateRangeTH();
		} else if (countryCode.equals("VN")) {
			return getDefaultSettlementDateRangeVN();
		}
		return 0;
	}
}