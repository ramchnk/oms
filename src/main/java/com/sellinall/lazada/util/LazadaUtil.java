package com.sellinall.lazada.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import org.apache.camel.Exchange;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.mudra.sellinall.config.Config;
import com.mudra.sellinall.database.DbUtilities;
import com.sellinall.lazada.common.LazadaOrderStatus;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.DateUtil;
import com.sellinall.util.HttpsURLConnectionUtil;
import com.sellinall.util.enums.SIAOrderCancelReasons;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.SIAPaymentStatus;
import com.sellinall.util.enums.SIAShippingStatus;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateDateModel;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;

/**
 * @author vikraman, Malli
 * @company sellinall
 * 
 */
public class LazadaUtil {
	static Logger log = Logger.getLogger(LazadaUtil.class.getName());
	private static MemcachedClient memcachedClient;
	
	public static final Map<String, String> timeZoneCountryMap = Collections
			.unmodifiableMap(new HashMap<String, String>() {
				private static final long serialVersionUID = 1L;

				{
					put("MY", "GMT+8");
					put("ID", "GMT+7");
					put("PH", "GMT+8");
					put("SG", "GMT+8");
					put("TH", "GMT+7");
					put("VN", "GMT+7");
				}
			});

	public static Object mapToQueryString(Map<String, String> queryString) {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> e : queryString.entrySet()) {
			if (sb.length() > 0) {
				sb.append('&');
			}
			try {
				sb.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=')
						.append(URLEncoder.encode(e.getValue(), "UTF-8"));
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return sb.toString();
	}

	public static String getFormattedCurrentTime() {
		DateTime now = new DateTime();
		DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();
		return fmt.print(now);
	}

	public static String getFormattedTime(long unixTime) {
		Date date = new Date(unixTime * 1000L);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		return df.format(date);
	}
	
	public static long getUnixTimeWithTimeZone(String date, String dateFormat, String country) {
		SimpleDateFormat df = new SimpleDateFormat(dateFormat);
		df.setTimeZone(TimeZone.getTimeZone(timeZoneCountryMap.get(country)));
		Date dateAndtime = new Date();
		try {
			dateAndtime = df.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return dateAndtime.getTime() / 1000;
	}

	public static long getUnixTimestamp(String time) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = new Date();
		try {
			date = df.parse(time);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return date.getTime() / 1000;
	}

	public static void initMemoryCached() {
		try {
			AuthDescriptor ad = new AuthDescriptor(new String[] { "PLAIN" }, new PlainCallbackHandler(Config
					.getConfig().getMemcachedCloudUsername(), Config.getConfig().getMemcachedCloudPassword()));

			MemcachedClient mc = new MemcachedClient(new ConnectionFactoryBuilder()
					.setProtocol(ConnectionFactoryBuilder.Protocol.BINARY).setAuthDescriptor(ad).build(),
					AddrUtil.getAddresses(Config.getConfig().getMemcachedCloudServers()));
			memcachedClient = mc;
		} catch (IOException ex) {
			log.error("Memcached client could not be initialized");
			ex.printStackTrace();
		}
	}
	
	public static MemcachedClient getMemcachedClient(){
		return memcachedClient;
	}

	public static String getMCkeyforGcStatusWaitingSKUS(String channelName, String accountNumber, String nickNameId) {
		return channelName + "-" + accountNumber + "-" + nickNameId + "-QcStatucWaitingSKUS";
	}
	
	public static Object getValueFromMemcache(String MCkey, boolean retry) {
		MemcachedClient mc = LazadaUtil.getMemcachedClient();
		try {
			Object mcValue = mc.get(MCkey);
			if (mcValue != null) {
				return mcValue;
			}
		} catch (Exception e) {
			if (retry) {
				try {
					log.error("Retrying to read value from  memcache, key:"+MCkey);
					mc.shutdown();
					LazadaUtil.initMemoryCached();
					return getValueFromMemcache(MCkey, false);
				} catch (Exception i) {
					i.printStackTrace();
				}
			} else {
				log.error("unable to get value from memcache, key :"+MCkey);
				e.printStackTrace();
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static String processItemDescription(BasicDBObject inventoryLazada, DBObject userDetails, String templateID,
			String inventoryDescription) {
		ArrayList<BasicDBObject> userTemplateList = (ArrayList<BasicDBObject>) userDetails.get("userTemplate");
		String templatePath = "";
		for (BasicDBObject userTemplate : userTemplateList) {
			BasicDBObject nickName = (BasicDBObject) userTemplate.get("nickName");
			if (nickName.getString("id").equals(templateID)) {
				templatePath = userTemplate.getString("templateUrl");
				break;
			}
		}
		try {
			String template = processUserTemplate(templatePath);
			inventoryLazada = processItemSpecific(inventoryLazada);
			inventoryLazada = processItemImages(inventoryLazada);
			inventoryLazada.put("inventoryDescription", inventoryDescription);
			Configuration cfg = new Configuration(Configuration.VERSION_2_3_21);
			cfg.setDefaultEncoding("UTF-8");
			DefaultObjectWrapperBuilder owb = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_21);
			owb.setDefaultDateType(TemplateDateModel.DATETIME);
			cfg.setObjectWrapper(owb.build());
			Template t = new Template("", new StringReader(template), cfg);
			Writer out = new StringWriter();
			t.process(inventoryLazada, out);
			return out.toString();
		} catch (Exception exception) {
			String errorMessage = exception.getMessage() == null ? "Template Error" : exception.getMessage();
			if (errorMessage.contains("==>")) {
				String errorLocation = errorMessage.split("----")[0];
				return "Failure-" + errorLocation.split("==>")[1];
			} else {
				log.error("Error occured while processing the description template");
				log.error(exception.toString());
				return "Failure-" + errorMessage;
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static BasicDBObject processItemImages(BasicDBObject inventoryLazada) {
		if (inventoryLazada.containsField("imageURI") && inventoryLazada.containsField("imageURL")) {
			ArrayList<String> images = (ArrayList<String>) inventoryLazada.get("imageURI");
			for (int i = 0; i < images.size(); i++) {
				inventoryLazada.put("image" + (i + 1), inventoryLazada.getString("imageURL") + images.get(i));
			}
		}
		return inventoryLazada;
	}

	@SuppressWarnings("unchecked")
	public static BasicDBObject processItemSpecific(BasicDBObject inventoryLazada) {
		BasicDBObject itemSpecific = new BasicDBObject();
		if (inventoryLazada.containsField("itemSpecifics")) {
			ArrayList<BasicDBObject> itemSpecifics = (ArrayList<BasicDBObject>) inventoryLazada.get("itemSpecifics");
			for (BasicDBObject inventoryItemSpec : itemSpecifics) {
				ArrayList<String> names = (ArrayList<String>) inventoryItemSpec.get("names");
				String title = inventoryItemSpec.getString("title").replace("normal.", "").replace("sku.", "");
				title = title.replaceAll("[^a-zA-Z0-9]", "_");
				for (String name : names) {
					// Currently we have only single string in names array
					itemSpecific.put(title, name);
				}
			}
		}
		inventoryLazada.put("itemSpecific", itemSpecific);
		return inventoryLazada;
	}

	public static String processUserTemplate(String templatePath) throws IOException, JSONException {
		String template = "";
		JSONObject response = HttpsURLConnectionUtil.doGet(templatePath, null);
		if (response.has("payload")) {
			template = response.getString("payload");
		}
		return template.replace("&lt;", "<").replace("&gt;", ">").replace("<!--", "</").replace("-->", ">");
	}

	private static final List<String> cancelReasonList = Arrays.asList(SIAOrderCancelReasons.OUT_OF_STOCK.toString(),
			SIAOrderCancelReasons.WRONG_PRODUCT_OR_PRICE_INFO.toString());

	public static List<String> getCancelReasonList() {
		return cancelReasonList;
	}

	public static JSONObject getCancelDetails(String cancelReason) throws JSONException {
		JSONObject cancelDetails = new JSONObject();
		if (cancelReason.equals(SIAOrderCancelReasons.OUT_OF_STOCK.toString())) {
			cancelDetails.put("cancelReason", "Out of Stock");
			cancelDetails.put("reasonID", "15");
		} else if (cancelReason.equals(SIAOrderCancelReasons.WRONG_PRODUCT_OR_PRICE_INFO.toString())) {
			cancelDetails.put("cancelReason", "Wrong Price or Pricing Error");
			cancelDetails.put("reasonID", "21");
		}
		return cancelDetails;
	}

	public static String getCategoryName(String countryCode, String categoryID){
		String url = Config.getConfig().getSIACategoryNameLookupURL() + "?channelName=lazada&countryCode=" + countryCode
				+ "&categoryID=" + categoryID;
		String categoryName ="";
		try{
			JSONObject response = HttpsURLConnectionUtil.doGet(url, null);
			if (response.has("payload")) {
				categoryName = response.getString("payload");
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return categoryName;
	}

	public static String getCancelReason(String lazadaCancelReason) {
		if (lazadaCancelReason.equalsIgnoreCase("Wrong Price or Pricing Error")
				|| lazadaCancelReason.equalsIgnoreCase("Item doesn't match description/pictures")
				|| lazadaCancelReason.equals("ร้านค้ายกเลิกรายการ - ราคาสินค้าไม่ถูกต้อง")) {
			return SIAOrderCancelReasons.WRONG_PRODUCT_OR_PRICE_INFO.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Out of Stock") || lazadaCancelReason.equalsIgnoreCase("สินค้าหมด")
				|| lazadaCancelReason.equalsIgnoreCase("Seller Kehabisan Stok")
				|| lazadaCancelReason.equals("ยกเลิกโดยร้านค้า เหตุผล: สินค้าหมด")
				|| lazadaCancelReason.equalsIgnoreCase("Cancelled by Seller - Out of Stock")
				|| lazadaCancelReason.equalsIgnoreCase("i was informed that item(s) in my order was out of stock")
				|| lazadaCancelReason.equalsIgnoreCase("Seller ran out of stock")) {
			return SIAOrderCancelReasons.OUT_OF_STOCK.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Change of mind") || lazadaCancelReason.equalsIgnoreCase("Berubah Pikiran")
				|| lazadaCancelReason.equalsIgnoreCase("เปลี่ยนใจ")) {
			return SIAOrderCancelReasons.CHANGE_OF_MIND.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("sourcing delay")) {
			return SIAOrderCancelReasons.SOURCING_DELAY.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Change/combine order")
				|| lazadaCancelReason.equalsIgnoreCase("Mengubah/menggabungkan pesanan")
				|| lazadaCancelReason.equalsIgnoreCase("Mengubah pesanan (menambah/mengurangi item)")
				|| lazadaCancelReason.equalsIgnoreCase("i want to create a new order to add/remove item(s)")) {
			return SIAOrderCancelReasons.CHANGED_OR_COMBINED_ORDER.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Found cheaper elsewhere")
				|| lazadaCancelReason.equalsIgnoreCase("Found better price elsewhere")
				|| lazadaCancelReason.equalsIgnoreCase("พบสินค้าอื่นที่ถูกหรือดีกว่า")
				|| lazadaCancelReason.equalsIgnoreCase("Lebih murah di tempat lain")) {
			return SIAOrderCancelReasons.FOUND_CHEAPER_ELSEWHERE.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Decided for alternative product")
				|| lazadaCancelReason.equalsIgnoreCase("Decided on another product")
				|| lazadaCancelReason.equalsIgnoreCase("Memutuskan Membeli Produk Lain")
				|| lazadaCancelReason.equalsIgnoreCase("ตัดสินใจเลือกสินค้าอื่น")) {
			return SIAOrderCancelReasons.DECIDED_ALTERNATIVE_PRODUCT.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Delivery time is too long")
				|| lazadaCancelReason.equalsIgnoreCase("Seller cancelled due to delay")
				|| lazadaCancelReason
						.equalsIgnoreCase("Seller is unable to send your package in time, and may not fulfill at all")
				|| lazadaCancelReason.equalsIgnoreCase("Waktu Pengiriman Terlalu Lama")
				|| lazadaCancelReason.equalsIgnoreCase("ม่สามารถจัดหาได้ตามกำหนด")
				|| lazadaCancelReason.equalsIgnoreCase("ไม่สามารถจัดหาได้ตามกำหนด")) {
			return SIAOrderCancelReasons.SHIPPING_DELAY.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Seller was unable to reserve your stock")) {
			return SIAOrderCancelReasons.SELLER_UNABLE_TO_RESERVE_STOCK.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Payment unsuccessful - payment time out\n")
				|| lazadaCancelReason.equalsIgnoreCase("Melebihi batas waktu pembayaran")
				|| lazadaCancelReason.equalsIgnoreCase("Payment unsuccessful - time limit reached")
				|| lazadaCancelReason.equalsIgnoreCase("Payment unsuccessful - payment time out")
				|| lazadaCancelReason.equalsIgnoreCase("Payment not completed on time")
				|| lazadaCancelReason.equals("ชำระเงินไม่สำเร็จ - เกินเวลาที่กำหนดให้ชำระ")) {
			return SIAOrderCancelReasons.PAYMENT_NOT_COMPLETED_IN_TIME.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Change payment method")
				|| lazadaCancelReason.equalsIgnoreCase("ต้องการเปลี่ยนวิธีการชำระเงิน")) {
			return SIAOrderCancelReasons.PAYMENT_PROBLEM.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Change of Delivery Address")
				|| lazadaCancelReason.equalsIgnoreCase("Mengubah alamat pengiriman")
				|| lazadaCancelReason.equalsIgnoreCase("ต้องการเปลี่ยนข้อมูลการจัดส่ง")
				|| lazadaCancelReason.equalsIgnoreCase("i need to change my delivery address")) {
			return SIAOrderCancelReasons.DELIVERY_ADDRESS_CHANGE.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Seller cancelled due to incorrect pricing")) {
			return SIAOrderCancelReasons.WRONG_PRICE.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Duplicate order") || lazadaCancelReason.equalsIgnoreCase("Pemesanan ganda")
				|| lazadaCancelReason.equalsIgnoreCase("คำสั่งซื้อซ้ำ")
				|| lazadaCancelReason.equalsIgnoreCase("Duplicated order")) {
			return SIAOrderCancelReasons.DUPLICATE_ORDER.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Shipping cost too high")
				|| lazadaCancelReason.equalsIgnoreCase("Fees - shipping costs")) {
			return SIAOrderCancelReasons.SHIPPING_COST_TOO_HIGH.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Change payment method")
				|| lazadaCancelReason.equalsIgnoreCase("Mengubah metode pembayaran")) {
			return SIAOrderCancelReasons.CHANGE_PAYMENT_METHOD.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("incorrect contact information")) {
			return SIAOrderCancelReasons.BUYER_CANCEL_OR_ADDRESS_ISSUE.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("canceled by customer")
				|| lazadaCancelReason.equalsIgnoreCase("I don't want the item any more")
				|| lazadaCancelReason.isEmpty()) {
			return SIAOrderCancelReasons.CANCELLED_BY_BUYER_OR_CANCEL_REASON_NOT_EXISTS.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Seller failed to fulfill your order on time")
				|| lazadaCancelReason.equalsIgnoreCase("Seller tidak dapat memenuhi pesanan tepat waktu")
				|| lazadaCancelReason.equalsIgnoreCase("The seller failed to ship out your parcel on time. The order is cancelled automatically by Lazada.")
				|| lazadaCancelReason.equalsIgnoreCase("Seller failed to handover the package to our logistics partner")
				|| lazadaCancelReason.equalsIgnoreCase("Order was cancelled due to slow fulfilment by the Seller. Customer would be refunded. Seller penalties may be applied, if deemed applicable")
				|| lazadaCancelReason.equalsIgnoreCase("Cancelled by Seller - Unable to fulfil order")) {
			return SIAOrderCancelReasons.SELLER_FAILED_TO_FULFILL_ORDER_ON_TIME.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Unexpected technical issue has prevented us from fulfilling your order")
				|| lazadaCancelReason.equalsIgnoreCase("Terjadi gangguan saat memproses pesanan")) {
			return SIAOrderCancelReasons.UNEXPECTED_TECHNICAL_ISSUE.toString();
		} else if (lazadaCancelReason.equalsIgnoreCase("Others")) {
			return SIAOrderCancelReasons.OTHERS.toString();
		}
		return "";
	}

	public static final Map<String, String> currencyToCountryCodeMap = Collections
			.unmodifiableMap(new HashMap<String, String>() {
				private static final long serialVersionUID = 1L;
				{
					put("SGD", "SG");
					put("TWD", "TW");
					put("IDR", "ID");
					put("MYR", "MY");
					put("THB", "TH");
					put("VND", "VN");
					put("PHP", "PH");
				}
			});

	public static final Map<String, String> countryCodeToCurrencyMap = Collections
			.unmodifiableMap(new HashMap<String, String>() {
				private static final long serialVersionUID = 1L;
				{
					put("SG", "SGD");
					put("TW", "TWD");
					put("ID", "IDR");
					put("MY", "MYR");
					put("TH", "THB");
					put("VN", "VND");
					put("PH", "PHP");
				}
			});

	public static String getSOFToken(String userName, String password, String countryCode)
			throws JSONException, IOException {
		Map<String, String> config = new HashMap<String, String>();
		config.put("Content-Type", "application/json");
		JSONObject payloads = new JSONObject();
		payloads.put("username", userName);
		payloads.put("password", password);
		String url = Config.getConfig().getSOFPortalUrl(countryCode);
		if (url.isEmpty()) {
			log.error("SOF not implemented for this country : " + countryCode);
			return "";
		}
		url = url + "auth/";
		JSONObject SOFResponse = HttpsURLConnectionUtil.doPost(url, payloads.toString(), config);
		if (SOFResponse.getInt("httpCode") == HttpStatus.SC_OK) {
			JSONObject response = new JSONObject(SOFResponse.getString("payload"));
			return response.getString("token");
		}
		return "";
	}

	public static JSONArray getVariantsFromCategory(JSONObject lookupJSON) throws JSONException {
		if (lookupJSON.has("variants")) {
			return lookupJSON.getJSONArray("variants");
		}
		return new JSONArray();

	}

	public static JSONArray getCategoryAttributes(JSONObject lookupJSON) throws JSONException {
		if (lookupJSON.has("attributes")) {
			return lookupJSON.getJSONArray("attributes");
		}
		return new JSONArray();
	}

	public static HashMap<String, String> constructAttributesAndAttributeTypeMap(JSONArray attributes)
			throws JSONException {
		HashMap<String, String> attributeAndTypeMap = new HashMap<String, String>();
		if (attributes.length() > 0) {
			for (int attributesIndex = 0; attributesIndex < attributes.length(); attributesIndex++) {
				JSONObject attribute = attributes.getJSONObject(attributesIndex);
				String appendedValue = attribute.getString("attribute_type") + "." + attribute.getString("name");
				attributeAndTypeMap.put(attribute.getString("name"), appendedValue);
			}
		}
		return attributeAndTypeMap;
	}

	public static JSONObject getShippingTrackingDetails(JSONObject orderFromDB, String courierName, String airwayBill,
			Exchange exchange, String orderItemID) throws JSONException {
		JSONObject shippingTrackingDetails = new JSONObject();
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		shippingTrackingDetails.put("airwayBill", airwayBill);
		String countryCode = exchange.getProperty("countryCode", String.class);
		if (courierName.contains("Seller Own Fleet") || courierName.contains("Seller Own")
				|| courierName.contains("SOFP") || courierName.contains("Delivered by Seller")
				|| courierName.contains("seller_own_fleet")) {
			if (!courierName.contains("Delivered by Seller")) {
				if (countryCode.equals("MY")) {
					courierName = "Seller Own";
				} else if (countryCode.equals("SG")) {
					courierName = "Seller Own Fleet";
				} else if (countryCode.equals("ID")) {
					String shippingProviderType = null;
					if (orderItemID != null) {
						shippingProviderType = getShippingProviderType(orderFromDB, orderItemID);
					}
					courierName = shippingProviderType != null ? shippingProviderType : "Seller Own";
				} else {
					log.info("Seller Own Fleet courierName : " + courierName + " , for : " + countryCode);
				}
			}
			String overrideTO = orderItemID != null ? "orderItemID : " + orderItemID
					: "orderID " + exchange.getProperty("orderID", String.class);
			shippingTrackingDetails.put("sofReferenceNumber", airwayBill);
			shippingTrackingDetails.remove("airwayBill");
			BasicDBObject channel = null;
			if (exchange.getProperties().containsKey("userChannel")) {
				channel = exchange.getProperty("userChannel", BasicDBObject.class);
			} else {
				channel = exchange.getProperty("channel", BasicDBObject.class);
			}
			boolean processOrdersWithSKUOnly = false;
			if (channel.containsField("processOrdersWithSKUOnly")) {
				processOrdersWithSKUOnly = channel.getBoolean("processOrdersWithSKUOnly");
			}
			if (orderFromDB.has("httpCode") && orderFromDB.getInt("httpCode") == 200) {
				JSONObject orderDetails = new JSONObject(orderFromDB.getString("payload"));
				// load orderItem shipping tracking details
				JSONObject shippingTrackingDetailsFromDB = null;
				if (orderItemID != null) {
					if (orderDetails.has("orderItems")) {
						JSONArray orderItems = orderDetails.getJSONArray("orderItems");
						for (int i = 0; i < orderItems.length(); i++) {
							JSONObject orderItem = orderItems.getJSONObject(i);
							if (orderItem.getString("orderItemID").equals(orderItemID)) {
								if (orderItem.has("shippingTrackingDetails")) {
									shippingTrackingDetailsFromDB = orderItem.getJSONObject("shippingTrackingDetails");
								} else {
									log.info("shippingTrackingDetails is not found for order item id : "
											+ orderItem.getString("orderItemID") + " for  accountNumber" + accountNumber
											+ " , nickname id is :" + exchange.getProperty("nickNameID", String.class));
								}
								break;
							}
						}
					}
				} else {
					JSONObject shippingDetailsFromDB = (JSONObject) orderDetails.get("shippingDetails");
					if (shippingDetailsFromDB.has("shippingTrackingDetails")) {
						shippingTrackingDetailsFromDB = (JSONObject) shippingDetailsFromDB
								.get("shippingTrackingDetails");
					}
				}
				if (shippingTrackingDetailsFromDB != null) {
					if (!shippingTrackingDetailsFromDB.has("sofReferenceNumber")) {
						shippingTrackingDetailsFromDB.put("sofReferenceNumber", airwayBill);
					}
					log.info(overrideTO + " already existing shipping tracking details  for  accountNumber"
							+ accountNumber + " and shipping tracking details"
							+ shippingTrackingDetailsFromDB.toString());
					shippingTrackingDetails = shippingTrackingDetailsFromDB;
				} else {
					shippingTrackingDetails.put("courierName", courierName);
				}
				log.info(overrideTO + " already exist in db so shipping tracking details override for  accountNumber "
						+ accountNumber + " and shipping tracking details" + shippingTrackingDetails.toString());
			} else if (orderFromDB.has("httpCode") && orderFromDB.getInt("httpCode") == 400
					&& orderFromDB.has("payload") && orderFromDB.getString("payload").startsWith("{")
					&& !processOrdersWithSKUOnly) {
				JSONObject payload = new JSONObject(orderFromDB.getString("payload"));
				log.warn("orderID : " + exchange.getProperty("orderID", String.class)
						+ " not found in our DB for accountNumber : " + accountNumber);
				String errorMsg = payload.getString("errorMessage");
				if (errorMsg.contains("orderID might have no data")) {
					/* Adding courierName for new orders */
					shippingTrackingDetails.put("courierName", courierName);
					log.info(overrideTO
							+ " not exist in db so shipping tracking details newly added  for  accountNumber "
							+ accountNumber + " and shipping tracking details" + shippingTrackingDetails.toString());
				}
			}
		} else {
			shippingTrackingDetails.put("courierName", courierName);
		}
		return shippingTrackingDetails;
	}

	public static void getStatusDetails(JSONArray statuses, String paymentMethod, JSONObject order,
			JSONObject orderFromDB, Exchange exchange, String type) throws JSONException, IOException {
		//TODO: need to be handled multiple notificationStatus
		String notificationStatus = statuses.getString(0);
		if (type.equals("order")) {
			notificationStatus = getOverallOrderStatus(statuses);
			getSIAOrderStatuses(statuses, order,paymentMethod);
		}
		if (paymentMethod.equals("COD")) {
			order.put("paymentStatus", SIAPaymentStatus.NOT_INITIATED);
		} else {
			order.put("paymentStatus", SIAPaymentStatus.COMPLETED);
		}
		log.debug("notification Status " + notificationStatus + "   " + LazadaOrderStatus.SHIPPED.name());
		if (LazadaOrderStatus.PENDING.equalsName(notificationStatus)
				|| LazadaOrderStatus.REPACKED.equalsName(notificationStatus)
				|| LazadaOrderStatus.PACKED.equalsName(notificationStatus)) {
			order.put("orderStatus", SIAOrderStatus.INITIATED);
			order.put("shippingStatus", SIAShippingStatus.NOT_SHIPPED);
		} else if (LazadaOrderStatus.READYTOSHIP.equalsName(notificationStatus)
				|| LazadaOrderStatus.READYTOSHIPPENDING.equalsName(notificationStatus)) {
			order.put("orderStatus", SIAOrderStatus.PROCESSING);
			order.put("shippingStatus", SIAShippingStatus.READY_TO_SHIP);
		} else if (LazadaOrderStatus.SHIPPED.equalsName(notificationStatus)) {
			order.put("orderStatus", SIAOrderStatus.DISPATCHED);
			order.put("shippingStatus", SIAShippingStatus.SHIPPED);
		} else if (LazadaOrderStatus.DELIVERED.equalsName(notificationStatus)) {
			order.put("orderStatus", SIAOrderStatus.DELIVERED);
			order.put("paymentStatus", SIAPaymentStatus.COMPLETED);
			order.put("shippingStatus", SIAShippingStatus.DELIVERED);
		} else if (LazadaOrderStatus.RETURN.equalsName(notificationStatus)
				|| LazadaOrderStatus.RETURNED.equalsName(notificationStatus)
				|| LazadaOrderStatus.PACKAGE_RETURNED.equalsName(notificationStatus)
				|| LazadaOrderStatus.BACK_TO_SHIPPER.equalsName(notificationStatus)
				|| LazadaOrderStatus.SHIPPER_RECEIVED.equalsName(notificationStatus)
				|| LazadaOrderStatus.SHIPPED_BACK_SUCCESS.equalsName(notificationStatus)) {
			order.put("orderStatus", SIAOrderStatus.RETURNED);
			order.put("paymentStatus", SIAPaymentStatus.REFUNDED);
			order.put("shippingStatus", SIAShippingStatus.RETURNED);
		} else if (LazadaOrderStatus.UNPAID.equalsName(notificationStatus)
				|| LazadaOrderStatus.UNVERIFIED.equalsName(notificationStatus)) {
			order.put("orderStatus", SIAOrderStatus.INITIATED);
			order.put("paymentStatus", SIAPaymentStatus.NOT_INITIATED);
			order.put("shippingStatus", SIAShippingStatus.NOT_SHIPPED);
		} else if (LazadaOrderStatus.FAILED.equalsName(notificationStatus)
				|| LazadaOrderStatus.DELIVERY_FAILED.equalsName(notificationStatus)
				|| LazadaOrderStatus.FAILED_DELIVERY.equalsName(notificationStatus)) {
			order.put("orderStatus", SIAOrderStatus.DELIVERY_FAILED);
			order.put("shippingStatus", SIAShippingStatus.UNKNOWN);
		} else if (LazadaOrderStatus.CANCELED.equalsName(notificationStatus)) {
			order.put("orderStatus", SIAOrderStatus.CANCELLED);
			order.put("paymentStatus", SIAPaymentStatus.REFUNDED);
			if (paymentMethod.isEmpty()) {
				order.put("paymentStatus", SIAPaymentStatus.NOT_INITIATED);
			}
			order.put("shippingStatus", SIAShippingStatus.NOT_SHIPPED);
			if (exchange.getProperties().containsKey("lazadaCancelReason")) {
				JSONObject cancelDetails = new JSONObject();
				if (orderFromDB.has("httpCode") && orderFromDB.getInt("httpCode") == 200) {
					JSONObject orderData = new JSONObject(orderFromDB.getString("payload"));
					if (orderData.has("cancelDetails")) {
						cancelDetails = orderData.getJSONObject("cancelDetails");
					}
				}
				String lazadaCancelreason = exchange.getProperty("lazadaCancelReason", String.class).trim();
				String cancelReason = LazadaUtil.getCancelReason(lazadaCancelreason);
				if (!cancelReason.isEmpty()) {
					cancelDetails.put("cancelReason", cancelReason);
					order.put("cancelDetails", cancelDetails);
				} else {
					log.error("Unhandled cancellation reason comes from lazada is: " + lazadaCancelreason
							+ " for orderID= " + exchange.getProperty("orderID", String.class));
				}
			}
		} else if (LazadaOrderStatus.RETURN_SHIPPED.equalsName(notificationStatus)
				|| LazadaOrderStatus.RETURN_SHIPPED_WITH_LAST_MILE.equalsName(notificationStatus)
				|| LazadaOrderStatus.SHIPPED_BACK.equalsName(notificationStatus)) {
			order.put("orderStatus", SIAOrderStatus.RETURN_SHIPPED);
			order.put("shippingStatus", SIAShippingStatus.UNKNOWN);
		} else if (LazadaOrderStatus.LOST_BY_3PL.equalsName(notificationStatus)
				|| LazadaOrderStatus.LOST_By_3PL.equalsName(notificationStatus)) {
			order.put("orderStatus", SIAOrderStatus.LOST_BY_3PL);
			order.put("paymentStatus", SIAPaymentStatus.REFUNDED);
			order.put("shippingStatus", SIAShippingStatus.SHIPPED);
		} else if (LazadaOrderStatus.DAMAGE_BY_3PL.equalsName(notificationStatus)
				|| LazadaOrderStatus.DAMAGED_BY_3PL.equalsName(notificationStatus)) {
			order.put("orderStatus", SIAOrderStatus.DAMAGE_BY_3PL);
			order.put("paymentStatus", SIAPaymentStatus.REFUNDED);
			order.put("shippingStatus", SIAShippingStatus.SHIPPED);
		} else if(LazadaOrderStatus.SCRAPPED.equalsName(notificationStatus)
				|| LazadaOrderStatus.PACKAGE_SCRAPPED.equalsName(notificationStatus)) {
			order.put("orderStatus", SIAOrderStatus.SCRAPPED);
			order.put("shippingStatus", SIAShippingStatus.UNKNOWN);
		} else if(LazadaOrderStatus.PACKAGE_RETURN_FAILED.equalsName(notificationStatus)
				|| LazadaOrderStatus.SHIPPED_BACK_FAILED.equalsName(notificationStatus)) {
			order.put("orderStatus", SIAOrderStatus.RETURN_FAILED);
			order.put("shippingStatus", SIAShippingStatus.UNKNOWN);
		} else {
			order.put("orderStatus", SIAOrderStatus.UNKNOWN);
			order.put("paymentStatus", SIAPaymentStatus.UNKNOWN);
			order.put("shippingStatus", SIAShippingStatus.UNKNOWN);
			log.warn("Invalid status for orderId: " + exchange.getProperty("orderID", String.class) + " is "
					+ notificationStatus);
		}
	}

	private static void getSIAOrderStatuses(JSONArray statuses, JSONObject order,String paymentMethod) throws JSONException {
		Set<String> SIAOrderStatuses = new HashSet<String>();
		Set<String> SIAShippingStatuses = new HashSet<String>();
		Set<String> SIAPaymentStatuses = new HashSet<String>();
		for (int i = 0; i < statuses.length(); i++) {
			String notificationStatus = statuses.getString(i);
			if (LazadaOrderStatus.PENDING.equalsName(notificationStatus)
					|| LazadaOrderStatus.REPACKED.equalsName(notificationStatus)
					|| LazadaOrderStatus.PACKED.equalsName(notificationStatus)) {
				SIAOrderStatuses.add(SIAOrderStatus.INITIATED.toString());
				SIAShippingStatuses.add(SIAShippingStatus.NOT_SHIPPED.toString());
				setPaymentStatus(SIAPaymentStatuses, paymentMethod);
			} else if (LazadaOrderStatus.READYTOSHIP.equalsName(notificationStatus)
					|| LazadaOrderStatus.READYTOSHIPPENDING.equalsName(notificationStatus)) {
				SIAOrderStatuses.add(SIAOrderStatus.PROCESSING.toString());
				SIAShippingStatuses.add(SIAShippingStatus.READY_TO_SHIP.toString());
				setPaymentStatus(SIAPaymentStatuses, paymentMethod);
			} else if (LazadaOrderStatus.SHIPPED.equalsName(notificationStatus)) {
				SIAOrderStatuses.add(SIAOrderStatus.DISPATCHED.toString());
				SIAShippingStatuses.add(SIAShippingStatus.SHIPPED.toString());
				setPaymentStatus(SIAPaymentStatuses, paymentMethod);
			} else if (LazadaOrderStatus.DELIVERED.equalsName(notificationStatus)) {
				SIAOrderStatuses.add(SIAOrderStatus.DELIVERED.toString());
				SIAShippingStatuses.add(SIAShippingStatus.DELIVERED.toString());
				SIAPaymentStatuses.add(SIAPaymentStatus.COMPLETED.toString());
			} else if (LazadaOrderStatus.RETURN.equalsName(notificationStatus)
					|| LazadaOrderStatus.RETURNED.equalsName(notificationStatus)
					|| LazadaOrderStatus.PACKAGE_RETURNED.equalsName(notificationStatus)
					|| LazadaOrderStatus.BACK_TO_SHIPPER.equalsName(notificationStatus)
					|| LazadaOrderStatus.SHIPPER_RECEIVED.equalsName(notificationStatus)
					|| LazadaOrderStatus.SHIPPED_BACK_SUCCESS.equalsName(notificationStatus)) {
				SIAOrderStatuses.add(SIAOrderStatus.RETURNED.toString());
				SIAShippingStatuses.add(SIAShippingStatus.RETURNED.toString());
				SIAPaymentStatuses.add(SIAPaymentStatus.REFUNDED.toString());
			} else if (LazadaOrderStatus.UNPAID.equalsName(notificationStatus)
					|| LazadaOrderStatus.UNVERIFIED.equalsName(notificationStatus)) {
				SIAOrderStatuses.add(SIAOrderStatus.INITIATED.toString());
				SIAShippingStatuses.add(SIAShippingStatus.NOT_SHIPPED.toString());
				SIAPaymentStatuses.add(SIAPaymentStatus.NOT_INITIATED.toString());
			} else if (LazadaOrderStatus.FAILED.equalsName(notificationStatus)
					|| LazadaOrderStatus.DELIVERY_FAILED.equalsName(notificationStatus)
					|| LazadaOrderStatus.FAILED_DELIVERY.equalsName(notificationStatus)) {
				SIAOrderStatuses.add(SIAOrderStatus.DELIVERY_FAILED.toString());
				SIAShippingStatuses.add(SIAShippingStatus.UNKNOWN.toString());
			} else if (LazadaOrderStatus.CANCELED.equalsName(notificationStatus)) {
				SIAOrderStatuses.add(SIAOrderStatus.CANCELLED.toString());
				SIAShippingStatuses.add(SIAShippingStatus.NOT_SHIPPED.toString());
				SIAPaymentStatuses.add(SIAPaymentStatus.REFUNDED.toString());
			} else if (LazadaOrderStatus.RETURN_SHIPPED.equalsName(notificationStatus)
					|| LazadaOrderStatus.RETURN_SHIPPED_WITH_LAST_MILE.equalsName(notificationStatus)
					|| LazadaOrderStatus.SHIPPED_BACK.equalsName(notificationStatus)) {
				SIAOrderStatuses.add(SIAOrderStatus.RETURN_SHIPPED.toString());
				SIAShippingStatuses.add(SIAShippingStatus.UNKNOWN.toString());
			} else if (LazadaOrderStatus.LOST_BY_3PL.equalsName(notificationStatus)
					|| LazadaOrderStatus.LOST_By_3PL.equalsName(notificationStatus)) {
				SIAOrderStatuses.add(SIAOrderStatus.LOST_BY_3PL.toString());
				SIAShippingStatuses.add(SIAShippingStatus.SHIPPED.toString());
				SIAPaymentStatuses.add(SIAPaymentStatus.REFUNDED.toString());
			} else if (LazadaOrderStatus.DAMAGE_BY_3PL.equalsName(notificationStatus)
					|| LazadaOrderStatus.DAMAGED_BY_3PL.equalsName(notificationStatus)) {
				SIAOrderStatuses.add(SIAOrderStatus.DAMAGE_BY_3PL.toString());
				SIAShippingStatuses.add(SIAShippingStatus.SHIPPED.toString());
				SIAPaymentStatuses.add(SIAPaymentStatus.REFUNDED.toString());
			} else if(LazadaOrderStatus.SCRAPPED.equalsName(notificationStatus)
					|| LazadaOrderStatus.PACKAGE_SCRAPPED.equalsName(notificationStatus)) {
				SIAOrderStatuses.add(SIAOrderStatus.SCRAPPED.toString());
				SIAShippingStatuses.add(SIAShippingStatus.UNKNOWN.toString());
				setPaymentStatus(SIAPaymentStatuses, paymentMethod);
			} else if(LazadaOrderStatus.PACKAGE_RETURN_FAILED.equalsName(notificationStatus)
					|| LazadaOrderStatus.SHIPPED_BACK_FAILED.equalsName(notificationStatus)) {
				SIAOrderStatuses.add(SIAOrderStatus.RETURN_FAILED.toString());
				SIAShippingStatuses.add(SIAShippingStatus.UNKNOWN.toString());
				setPaymentStatus(SIAPaymentStatuses, paymentMethod);
			} else {
				SIAOrderStatuses.add(SIAOrderStatus.UNKNOWN.toString());
				SIAShippingStatuses.add(SIAShippingStatus.UNKNOWN.toString());
				SIAPaymentStatuses.add(SIAPaymentStatus.UNKNOWN.toString());
			}
		}
		order.put("orderStatuses", SIAOrderStatuses);
		order.put("shippingStatuses", SIAShippingStatuses);
		order.put("paymentStatuses", SIAPaymentStatuses);
	}

	public static String getSIAOrderStatus(String status) throws JSONException {
		if (LazadaOrderStatus.PENDING.equalsName(status) || LazadaOrderStatus.REPACKED.equalsName(status)
				|| LazadaOrderStatus.PACKED.equalsName(status)) {
			return SIAOrderStatus.INITIATED.toString();
		} else if (LazadaOrderStatus.READYTOSHIP.equalsName(status)
				|| LazadaOrderStatus.READYTOSHIPPENDING.equalsName(status)) {
			return SIAOrderStatus.PROCESSING.toString();
		} else if (LazadaOrderStatus.SHIPPED.equalsName(status)) {
			return SIAOrderStatus.DISPATCHED.toString();
		} else if (LazadaOrderStatus.DELIVERED.equalsName(status)) {
			return SIAOrderStatus.DELIVERED.toString();
		} else if (LazadaOrderStatus.RETURN.equalsName(status) || LazadaOrderStatus.RETURNED.equalsName(status)
				|| LazadaOrderStatus.PACKAGE_RETURNED.equalsName(status)
				|| LazadaOrderStatus.BACK_TO_SHIPPER.equalsName(status)
				|| LazadaOrderStatus.SHIPPER_RECEIVED.equalsName(status)
				|| LazadaOrderStatus.SHIPPED_BACK_SUCCESS.equalsName(status)) {
			return SIAOrderStatus.RETURNED.toString();
		} else if (LazadaOrderStatus.UNPAID.equalsName(status) || LazadaOrderStatus.UNVERIFIED.equalsName(status)) {
			return SIAOrderStatus.INITIATED.toString();
		} else if (LazadaOrderStatus.FAILED.equalsName(status) || LazadaOrderStatus.DELIVERY_FAILED.equalsName(status)
				|| LazadaOrderStatus.FAILED_DELIVERY.equalsName(status)) {
			return SIAOrderStatus.DELIVERY_FAILED.toString();
		} else if (LazadaOrderStatus.CANCELED.equalsName(status)) {
			return SIAOrderStatus.CANCELLED.toString();
		} else if (LazadaOrderStatus.RETURN_SHIPPED.equalsName(status)
				|| LazadaOrderStatus.RETURN_SHIPPED_WITH_LAST_MILE.equalsName(status)
				|| LazadaOrderStatus.SHIPPED_BACK.equalsName(status)) {
			return SIAOrderStatus.RETURN_SHIPPED.toString();
		} else if (LazadaOrderStatus.LOST_BY_3PL.equalsName(status)
				|| LazadaOrderStatus.LOST_By_3PL.equalsName(status)) {
			return SIAOrderStatus.LOST_BY_3PL.toString();
		} else if (LazadaOrderStatus.DAMAGE_BY_3PL.equalsName(status)
				|| LazadaOrderStatus.DAMAGED_BY_3PL.equalsName(status)) {
			return SIAOrderStatus.DAMAGE_BY_3PL.toString();
		} else if (LazadaOrderStatus.SCRAPPED.equalsName(status)
				|| LazadaOrderStatus.PACKAGE_SCRAPPED.equalsName(status)) {
			return SIAOrderStatus.SCRAPPED.toString();
		} else if (LazadaOrderStatus.PACKAGE_RETURN_FAILED.equalsName(status)
				|| LazadaOrderStatus.SHIPPED_BACK_FAILED.equalsName(status)) {
			return SIAOrderStatus.RETURN_FAILED.toString();
		} else {
			return SIAOrderStatus.UNKNOWN.toString();
		}
	}

	public static void setPaymentStatus(Set<String> SIAPaymentStatuses, String paymentMethod) {
		if (paymentMethod.equals("COD")) {
			SIAPaymentStatuses.add(SIAPaymentStatus.NOT_INITIATED.toString());
		} else {
			SIAPaymentStatuses.add(SIAPaymentStatus.COMPLETED.toString());
		}
	}

	public static JSONObject getOrderDetails(Exchange exchange) throws JSONException, IOException {
		DBObject userDetails = (DBObject) exchange.getProperty("UserDetails");
		BasicDBObject channel = null;
		if (userDetails.get("lazada") instanceof List) {
			String nickNameID = exchange.getProperty("nickNameID", String.class);
			channel = getChannelObject(userDetails, nickNameID);
		} else {
			channel = (BasicDBObject) userDetails.get("lazada");
		}
		BasicDBObject nickName = (BasicDBObject) channel.get("nickName");
		String nickNameID = nickName.getString("id");
		String orderID = exchange.getProperty("orderID", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String url = Config.getConfig().getSIAOrderURL() + "/order/" + nickNameID + "/" + orderID;
		Map<String, String> map = new HashMap<String, String>();
		map.put("Content-Type", "application/json");
		map.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		map.put("accountNumber", accountNumber);
		JSONObject order = HttpsURLConnectionUtil.doGet(url, map);
		return order;
	}

	private static BasicDBObject getChannelObject(DBObject userDetails, String nickNameID) {
		List<BasicDBObject> channelArray = (List<BasicDBObject>) userDetails.get("lazada");
		for (int i = 0; i < channelArray.size(); i++) {
			BasicDBObject userChannel = channelArray.get(i);
			DBObject nickName = (DBObject) userChannel.get("nickName");
			if (nickName.get("id").toString().equals(nickNameID)) {
				return userChannel;
			}
		}
		return null;
	}

	public static List<String> getValidVariantOrder(JSONArray SKUs, JSONArray SIAVariantsDetails) throws JSONException {
		Map<Integer, String> vaildVariantOrder = new HashMap<Integer, String>();
		for (int i = 0; i < SIAVariantsDetails.length(); i++) {
			String variantName = SIAVariantsDetails.getString(i);
			for (int child = 0; child < SKUs.length(); child++) {
				JSONObject childDetails = SKUs.getJSONObject(child);
				JSONArray childDetailsKeys = childDetails.names();
				for (int index = 0; index < childDetailsKeys.length(); index++) {
					if (childDetailsKeys.getString(index).equals(variantName)) {
						vaildVariantOrder.put(index, variantName);
						break;
					}
				}
			}
		}
		List<String> values = new ArrayList<String>(vaildVariantOrder.values());
		return values;
	}

	public static String getOverallOrderStatus(JSONArray orderStatuses) throws JSONException {
		if (orderStatuses.length() == 1) {
			return orderStatuses.getString(0);
		}
		String arrayString = orderStatuses.toString();
		if (arrayString.contains(LazadaOrderStatus.PENDING.toString())
				|| arrayString.contains(LazadaOrderStatus.REPACKED.toString())
				|| arrayString.contains(LazadaOrderStatus.PACKED.toString())) {
			return LazadaOrderStatus.PENDING.toString();
		} else if (arrayString.contains(LazadaOrderStatus.READYTOSHIP.toString())
				|| arrayString.contains(LazadaOrderStatus.READYTOSHIPPENDING.toString())) {
			return LazadaOrderStatus.READYTOSHIP.toString();
		} else if (arrayString.contains(LazadaOrderStatus.SHIPPED.toString())) {
			return LazadaOrderStatus.SHIPPED.toString();
		} else if (arrayString.contains(LazadaOrderStatus.DELIVERED.toString())) {
			return LazadaOrderStatus.DELIVERED.toString();
		} else if (arrayString.contains(LazadaOrderStatus.CANCELED.toString())) {
			return LazadaOrderStatus.CANCELED.toString();
		} else if (arrayString.contains(LazadaOrderStatus.RETURN.toString())
				|| arrayString.contains(LazadaOrderStatus.RETURNED.toString())) {
			return LazadaOrderStatus.RETURNED.toString();
		} else if (arrayString.contains(LazadaOrderStatus.UNPAID.toString())
				|| arrayString.contains(LazadaOrderStatus.UNVERIFIED.toString())) {
			return LazadaOrderStatus.UNPAID.toString();
		} else if (arrayString.contains(LazadaOrderStatus.FAILED.toString())) {
			return LazadaOrderStatus.FAILED.toString();
		} else {
			return orderStatuses.getString(0);
		}
	}

	public static final Map<String, String> lazadaOrderReasonMap = Collections
			.unmodifiableMap(new HashMap<String, String>() {
				private static final long serialVersionUID = 1L;

				{
					put("cancellation-internal", "CANCELLATION_INTERNAL");
					put("cancellation-customer", "CANCELLATION_CUSTOMER");
					put("cancellation-failed Delivery", "CANCELLATION_FAILED_DELIVERY");
					put("cancellation-seller", "CANCELLATION_SELLER");
					put("return-customer", "RETURN_CUSTOMER");
					put("refund-internal", "REFUND_INTERNAL");
					put("buyer-return", "RETURN_CUSTOMER");
					put("customer service-only_refund", "CUSTOMER_SERVICE_ONLY_REFUND");
					put("seller-cancel", "CANCELLATION_SELLER");
					put("buyer-cancel", "CANCELLATION_CUSTOMER");
					put("system-cancel", "SYSTEM_CANCEL");
				}
			});

	public static int loadSoldCount(String accountNumber, String customSKU, String nickNameID) throws IOException, JSONException {
		Map<String, String> config = new HashMap<String, String>();
		config.put("Content-Type", "application/json");
		config.put("accountNumber", accountNumber);
		config.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		String url = Config.getConfig().getSIAOrderURL() + "/orders/" + URLEncoder.encode(customSKU, "UTF-8")
				+ "/soldCount";
		url += "?orderStatus=INITIATED&siteNicknameId=" + nickNameID;
		JSONObject response = new JSONObject();
		try {
			response = HttpsURLConnectionUtil.doGet(url, config);
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
		int httpCode = response.getInt("httpCode");
		if (httpCode != HttpStatus.SC_OK) {
			log.error("Failed to load sold count for customSKU : " + customSKU + ", response : " + response);
			return 0;
		}
		JSONObject responsePayload = new JSONObject(response.getString("payload"));
		if (responsePayload.has("errorMessage")) {
			return 0;
		}
		return responsePayload.getInt("soldCount");
	}
	
	public static JSONObject getItemDetailsBySellerSKU(Exchange exchange)
	{
		HashMap<String, String> map = new HashMap<String, String>();
		String response = "";
		String parentItemId = "";
		String sellerSku = "";
		String queryParam = "";
		if (exchange.getProperties().containsKey("sellerSKU")) {
			sellerSku = exchange.getProperty("sellerSKU", String.class);
		}
		if (exchange.getProperties().containsKey("itemID")) {
			parentItemId = exchange.getProperty("itemID", String.class);
			queryParam = "&item_id=" + parentItemId;
			map.put("item_id", parentItemId);
		} else if (sellerSku != "") {
			queryParam = "&seller_sku=" + sellerSku;
			map.put("seller_sku", sellerSku);
		} else {
			log.error("Need sellerSku or itemId to get itemDetails " + "for accountNumber : "
					+ exchange.getProperty("accountNumber", String.class) + " for nickNameID: "
					+ exchange.getProperty("nickNameID"));
			return new JSONObject();
		}
		String accessToken = exchange.getProperty("accessToken", String.class);
		String hostUrl = exchange.getProperty("hostURL", String.class);
		map.put("access_token", accessToken);
		String apiName = "/product/item/get";
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		try {
			response = NewLazadaConnectionUtil.callAPI(hostUrl, apiName, accessToken, map, "", queryParam, "GET",
					clientID, clientSecret);
			if (!response.startsWith("{")) {
				log.error("Invalid item response for accountNumber : "
						+ exchange.getProperty("accountNumber", String.class) + " for nickNameID: "
						+ exchange.getProperty("nickNameID")
						+ (!parentItemId.isEmpty() ? " for itemId:" + parentItemId : " for sellerSku:" + sellerSku)
						+ " response is:" + response.toString());
				return new JSONObject();
			}
			JSONObject channelResponse = new JSONObject(response);
			if (!channelResponse.has("data")
					|| (channelResponse.has("code") && !channelResponse.getString("code").equals("0"))) {
				log.error("Invalid item response for accountNumber : "
						+ exchange.getProperty("accountNumber", String.class) + " for nickNameID: "
						+ exchange.getProperty("nickNameID")
						+ (!parentItemId.isEmpty() ? " for itemId:" + parentItemId : " for sellerSku:" + sellerSku)
						+ " response is:" + response.toString());
				return new JSONObject();
			}
			if (channelResponse.getJSONObject("data").has("skus")) {
				JSONArray skus = channelResponse.getJSONObject("data").getJSONArray("skus");
				for (int i = 0; i < skus.length(); i++) {
					JSONObject itemResponse = skus.getJSONObject(i);
					if ((itemResponse.has("SkuId") && exchange.getProperties().containsKey("skuID")
							&& itemResponse.getString("SkuId").equals(exchange.getProperty("skuID", String.class)))
							|| (itemResponse.has("SellerSku") && itemResponse.get("SellerSku").equals(sellerSku))) {
						return itemResponse;
					}
				}
			} else {
				log.error("skus not found for accountNumber : " + exchange.getProperty("accountNumber", String.class)
						+ " for nickNameID: " + exchange.getProperty("nickNameID")
						+ (!parentItemId.isEmpty() ? " for itemId:" + parentItemId : " for sellerSku:" + sellerSku)
						+ " response is:" + response.toString());
			}
		} catch (Exception e) {
			log.error("Internal error occured for get item details "
					+ (!parentItemId.isEmpty() ? "for itemId - " + parentItemId : "for sellerSku:" + sellerSku)
					+ " ,for accountNumber: " + exchange.getProperty("accountNumber", String.class)
					+ " ,for nickNameID: " + exchange.getProperty("nickNameID") + " and response: " + response);
			e.printStackTrace();
		}
		return new JSONObject();
	}

	public static void setCurrentOrderStatus(Exchange exchange, JSONObject order, ArrayList<String> orderItemIDs,
			boolean orderHasMultiplePackages) throws JSONException {
		// order status
		order.put("orderStatus", exchange.getProperty("currentOrderStatus", String.class));
		// OrderStatuses array
		if (exchange.getProperties().containsKey("currentOrderStatuses")) {
			order.put("orderStatuses", exchange.getProperty("currentOrderStatuses", JSONArray.class));
		}
		// shipping status
		if (exchange.getProperties().containsKey("currentShippingStatus")) {
			order.put("shippingStatus", exchange.getProperty("currentShippingStatus", String.class));
		}
		// shipping statues array
		if (exchange.getProperties().containsKey("currentShippingStatuses")) {
			order.put("shippingStatuses", exchange.getProperty("currentShippingStatuses", JSONArray.class));
		}
		if (orderHasMultiplePackages) {
			/*
			 * Note: if we got failure response for any package, then we need to add current
			 * status into statuses field
			 */
			if (order.has("orderStatuses")) {
				JSONArray orderStatuses = order.getJSONArray("orderStatuses");
				orderStatuses.put(exchange.getProperty("currentOrderStatus", String.class));
			}
			if (order.has("shippingStatuses")) {
				JSONArray orderStatuses = order.getJSONArray("shippingStatuses");
				if (exchange.getProperties().containsKey("currentShippingStatus")) {
					orderStatuses.put(exchange.getProperty("currentShippingStatus", String.class));
				}
			}
		}
		// orderItem status
		JSONArray orderItems = order.getJSONArray("orderItems");
		JSONObject orderItemStatusObj = exchange.getProperty("currentOrderItemStatusObj", JSONObject.class);
		for (int i = 0; i < orderItems.length(); i++) {
			JSONObject orderItem = orderItems.getJSONObject(i);
			String orderItemID = orderItem.getString("orderItemID");
			if (orderItemIDs.contains(orderItemID) && orderItemStatusObj.has(orderItemID)) {
				JSONObject currentStatusObejct = orderItemStatusObj.getJSONObject(orderItemID);
				boolean isStatusMissed = false;
				if (currentStatusObejct.has("orderStatus")) {
					orderItem.put("orderStatus", currentStatusObejct.get("orderStatus"));
				} else {
					isStatusMissed = true;
				}
				if (currentStatusObejct.has("shippingStatus")) {
					orderItem.put("shippingStatus", currentStatusObejct.get("shippingStatus"));
				} else {
					isStatusMissed = true;
				}
				if (isStatusMissed) {
					log.error("order/shipping status missed for orderItem : " + orderItemID + "orderID : "
							+ order.getString("orderID") + ", currentStatusObejct : " + currentStatusObejct
							+ ", accountNumber : " + exchange.getProperty("accountNumber", String.class)
							+ " for nickNameID: " + exchange.getProperty("nickNameID"));
				}
			}
		}

	}

	public static String getShippingProviderType(JSONObject order, String orderItemID) {
		String orderID = "";
		try {
			orderID = order.getString("orderID");
			JSONArray orderItems = order.getJSONArray("orderItems");
			for (int i = 0; i < orderItems.length(); i++) {
				JSONObject orderItem = orderItems.getJSONObject(i);
				// get first order shipping shippingProviderType that is enough
				if (orderItemID.equals(orderItem.getString("orderItemID"))) {
					return orderItem.getString("shippingProviderType");
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("Error occured while getting shippingProviderType for orderID " + orderID);
			e.printStackTrace();
		}
		return null;
	}

	public static String getPreviousDate(long unixtime, int day, String countryCode) {
		/* here day is DateTimeConstants */
		DateTime now;
		if (!countryCode.isEmpty()) {
			List<String> timezone = Arrays.asList(timeZoneCountryMap.get(countryCode).replace("GMT", "").split(":"));
			int hoursOffset = 0, minOffset = 0;
			if (timezone.size() > 0) {
				hoursOffset = Integer.parseInt(timezone.get(0));
				if (timezone.size() > 1) {
					minOffset = Integer.parseInt(timezone.get(1));
				}
			}
			DateTimeZone zone = DateTimeZone.forOffsetHoursMinutes(hoursOffset, minOffset);
			now = new DateTime(unixtime * 1000L, zone);
		} else {
			DateTimeZone zone = DateTimeZone.UTC;
			now = new DateTime(unixtime * 1000L, zone);
		}
		int offset = ((now.getDayOfWeek() - day) + 7) % 7;
		DateTime previousDay = now.minusDays(offset);
		return previousDay.toString("yyyy-MM-dd");
	}

	public static JSONArray parseToJsonArray(DBCursor myList) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		while (myList.hasNext()) {
			BasicDBObject doc = (BasicDBObject) myList.next();
			JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
			jsonArray.put(new JSONObject(doc.toJson(writerSettings)));
		}
		return jsonArray;
	}

	public static JSONArray parseListToJsonArray(BasicDBList myList) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (int i = 0; i < myList.size(); i++) {
			BasicDBObject doc = (BasicDBObject) myList.get(i);
			JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
			jsonArray.put(new JSONObject(doc.toJson(writerSettings)));
		}
		return jsonArray;
	}
	
	public static JSONObject parseToJsonObject(DBObject findOneObject) throws JSONException {
		if (findOneObject == null) {
			return new JSONObject();
		}
		BasicDBObject doc = (BasicDBObject) findOneObject;
		JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
		return new JSONObject(doc.toJson(writerSettings));
	}

	public static String removeSpecialCharacters(String feeName) {
		/*
		 * here we are replacing special characters & modified feeName into proper
		 * camelcase
		 */
		feeName = feeName.replaceAll("[^a-zA-Z\\s]", " ");
		StringBuilder modifiedFeeName = new StringBuilder();
		List<String> strArr = Arrays.asList(feeName.split(" "));
		for (int i = 0; i < strArr.size(); i++) {
			if (strArr.get(i).isEmpty()) {
				continue;
			}
			if (modifiedFeeName.length() == 0) {
				modifiedFeeName.append(strArr.get(i).substring(0, 1).toLowerCase());
			} else {
				modifiedFeeName.append(strArr.get(i).substring(0, 1).toUpperCase());
			}
			modifiedFeeName.append(strArr.get(i).substring(1, strArr.get(i).length()));
		}
		return modifiedFeeName.toString();
	}

	public static long getOrderTimeValues(JSONObject order, String apiFieldName, String countryCode)
			throws JSONException {
		if (order.has(apiFieldName)) {
			long timeOrderCreated = 0;
			if (order.getString(apiFieldName).split(" ").length > 1) {
				timeOrderCreated = DateUtil.getUnixTimestamp(order.getString(apiFieldName), "yyyy-MM-dd HH:mm:ss Z",
						order.getString(apiFieldName).split(" ")[2]);
			} else if (order.getString(apiFieldName).split("\\+").length > 1) {
				timeOrderCreated = LazadaUtil.getUnixTimeWithTimeZone(order.getString(apiFieldName).split("\\+")[0],
						"yyyy-MM-dd'T'HH:mm:ss", countryCode);
			} else {
				timeOrderCreated = LazadaUtil.getUnixTimeWithTimeZone(order.getString(apiFieldName),
						"yyyy-MM-dd HH:mm:ss", countryCode);
			}
			return timeOrderCreated;
		} else {
			return (Long) DateUtil.getSIADateFormat();
		}
	}

	public static JSONObject getCurrentStatusObj(JSONArray orderItems) throws JSONException {
		JSONObject currentItemStatusObj = new JSONObject();
		for (int i = 0; i < orderItems.length(); i++) {
			JSONObject orderItem = orderItems.getJSONObject(i);
			JSONObject currentStatusObject = new JSONObject();
			if (orderItem.has("currentOrderStatus")) {
				currentStatusObject.put("orderStatus", orderItem.getString("currentOrderStatus"));
			} else if (orderItem.has("orderStatus")) {
				currentStatusObject.put("orderStatus", orderItem.getString("orderStatus"));
			}
			if (orderItem.has("currentShippingStatus")) {
				currentStatusObject.put("shippingStatus", orderItem.getString("currentShippingStatus"));
			} else if (orderItem.has("shippingStatus")) {
				currentStatusObject.put("shippingStatus", orderItem.getString("shippingStatus"));
			}
			if (currentStatusObject.length() > 0) {
				currentItemStatusObj.put(orderItem.getString("orderItemID"), currentStatusObject);
			}
		}
		return currentItemStatusObj;
	}

	public static List<String> JSONArrayToStringList(JSONArray arrayList) throws JSONException {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < arrayList.length(); i++) {
			list.add(arrayList.getString(i));
		}
		return list;
	}


	public static DBObject getAccountDetails(String sellerId) {
		DBCollection table = DbUtilities.getDBCollection("accounts");

		BasicDBObject elemMatchQuery = new BasicDBObject();
		elemMatchQuery.put("postHelper.sellerID", sellerId);

		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("$elemMatch", elemMatchQuery);

		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("lazada", elemMatch);
		BasicDBObject projection = new BasicDBObject();
		projection.put("lazada.$", 1);
		projection.put("merchantID", 1);

		return table.findOne(searchQuery, projection);
  }
  
	public static void loadItemIDFromDB(Exchange exchange) {
		DBObject searchQuery = new BasicDBObject();
		try {
			String accountNumber = exchange.getProperty("accountNumber", String.class);
			String nickNameID = exchange.getProperty("nickNameID", String.class);
			String SKU = exchange.getProperty("SKU", String.class).split("-")[0];

			BasicDBObject elemMatch = new BasicDBObject();
			elemMatch.put("nickNameID", nickNameID);

			searchQuery.put("accountNumber", accountNumber);
			searchQuery.put("SKU", SKU);
			searchQuery.put("lazada", new BasicDBObject("$elemMatch", elemMatch));

			DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
			DBObject obj = table.findOne(searchQuery);

			if (obj != null) {
				List<DBObject> lazadaList = (List<DBObject>) obj.get("lazada");
				DBObject lazadaObj = lazadaList.get(0);
				if (lazadaObj.containsField("itemID")) {
					exchange.setProperty("itemID", lazadaObj.get("itemID").toString());
				}
			}
		} catch (Exception e) {
			log.error("Error occured while getting ItemID, searchQuery is:" + searchQuery);
			e.printStackTrace();
		}
	}

	public static JSONObject buildOrderAddendumObj(boolean isAutoAcceptOrder) throws JSONException {
		JSONObject addendum = new JSONObject();
		addendum.put("eventTime", System.currentTimeMillis() / 1000);
		if (isAutoAcceptOrder) {
			addendum.put("eventType", "AUTO_ACCEPT_UPDATE");
			addendum.put("adjustedBy", "SIA System");
		} else {
			addendum.put("eventType", "MARKETPLACE_UPDATE");
			addendum.put("adjustedBy", "Lazada Marketplace");
		}
		return addendum;
	}

	public static DBObject getChannelObjBySellerID(List<DBObject> lazadaObjList, String sellerID) {
		DBObject channelObj = null;
		for (DBObject lazadaObj : lazadaObjList) {
			DBObject postHelper = (DBObject) lazadaObj.get("postHelper");
			if (postHelper.containsField("sellerIDList")) {
				List<DBObject> sellerIDList = (List<DBObject>) postHelper.get("sellerIDList");
				for (DBObject sellerIDObj : sellerIDList) {
					if (sellerIDObj.get("sellerID").toString().equals(sellerID)) {
						channelObj = lazadaObj;
						break;
					}
				}
			} else if (postHelper.containsField("sellerID") && postHelper.get("sellerID").toString().equals(sellerID)) {
				channelObj = lazadaObj;
			}
			if (channelObj != null) {
				break;
			}
		}
		return channelObj;
	}

	public static boolean checkIsEligibleToProcessOrder(Exchange exchange, JSONObject order)
			throws IOException, JSONException {
		boolean isEligibleToProceed = true;
		try {
			JSONObject orderFromDB = getOrderDetails(exchange);
			if (orderFromDB.has("httpCode") && orderFromDB.getInt("httpCode") == 200) {
				JSONObject orderDetails = new JSONObject(orderFromDB.getString("payload"));
				JSONArray orderItems = orderDetails.has("orderItems") ? orderDetails.getJSONArray("orderItems")
						: new JSONArray();
				for (int j = 0; j < orderItems.length(); j++) {
					JSONObject orderItem = orderItems.getJSONObject(j);
					if (orderItem.has("shippingTrackingDetails")
							&& orderItem.getJSONObject("shippingTrackingDetails").has("packageID")
							&& !orderItem.getJSONObject("shippingTrackingDetails").getString("packageID").isEmpty()) {
						isEligibleToProceed = false;
						break;
					}
				}
				if (isEligibleToProceed && orderDetails.has("shippingDetails")) {
					JSONObject shippingDetails = orderDetails.getJSONObject("shippingDetails");
					if (shippingDetails.has("shippingTrackingDetails")
							&& shippingDetails.getJSONObject("shippingTrackingDetails").has("packageID")
							&& !shippingDetails.getJSONObject("shippingTrackingDetails").getString("packageID")
									.isEmpty()) {
						isEligibleToProceed = false;
					}
					if (isEligibleToProceed && shippingDetails.has("shippingTrackingDetailsList")) {
						JSONArray shippingTrackingDetailsList = shippingDetails
								.getJSONArray("shippingTrackingDetailsList");
						for (int i = 0; i < shippingTrackingDetailsList.length(); i++) {
							JSONObject shippingTrackingDetailObj = shippingTrackingDetailsList.getJSONObject(i);
							if (shippingTrackingDetailObj.has("packageID")
									&& !shippingTrackingDetailObj.getString("packageID").isEmpty()) {
								isEligibleToProceed = false;
								break;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return isEligibleToProceed;
	}

	public static DBObject getSKUDetails(String SKU) {
		DBObject searchQuery = new BasicDBObject();
		searchQuery.put("SKU", SKU);

		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		return table.findOne(searchQuery);
	}

	public static JSONObject getListingQuantities(String accountNumber, String nickNameID, String sellerSKU,
			String SKU) throws IOException, JSONException {
		String url = Config.getConfig().getInventoryUrl() + "/productMaster/listing/quantities/" + nickNameID
				+ "?sellerSKU=" + URLEncoder.encode(sellerSKU);
		if (!SKU.isEmpty()) {
			url += "&SKU=" + SKU;
		}
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		headers.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		headers.put("accountNumber", accountNumber);
		JSONObject response = HttpsURLConnectionUtil.doGet(url, headers);
		if (response.has("httpCode") && response.getInt("httpCode") == HttpStatus.SC_OK) {
			return new JSONObject(response.getString("payload"));
		}
		log.error("Error occured while calling listing quantities API for accountNumber: " + accountNumber
				+ ", nickNameID: " + nickNameID + ", sellerSKU: " + sellerSKU + ", response:" + response);
		return null;
	}

	public static void updateStockFeeds(String accountNumber, String nickNameID, BasicDBList feedList)
			throws JSONException {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("nickNameID", nickNameID);
		searchQuery.put("requestType", "stockUpdate");
		BasicDBObject updateData = new BasicDBObject();
		updateData.put("$push", new BasicDBObject("updateStockList", new BasicDBObject("$each", feedList)));
		DBCollection table = DbUtilities.getInventoryDBCollection("lazadaFeeds");
		WriteResult output = table.update(searchQuery, updateData, true, false);
		if (output.getN() == 0) {
			log.error("Failed while update the feed data for accountNumber : " + accountNumber + " nickNameID : "
					+ nickNameID + ", searchQuery : " + searchQuery + " & upsertData : " + updateData);
		}
	}
}