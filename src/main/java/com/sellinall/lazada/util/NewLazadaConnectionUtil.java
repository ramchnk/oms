package com.sellinall.lazada.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.mudra.sellinall.config.Config;

public class NewLazadaConnectionUtil {

	private static boolean ignoreSSLCheck = true; // ignore SSL check when
													// establish connection
	private static boolean ignoreHostCheck = true; // ignore HOST check when
													// establish connection
	protected static int connectTimeout = 15000; // default connection timeout
	protected static int readTimeout = 30000; // default read timeout
	private static String signMethod = "hmac";

	public static void main(String[] args) throws IOException {
		Config.context = new ClassPathXmlApplicationContext("Propertycfg.xml");
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("code", "0_kR5388iiJn5BF1xUcI1SXB4p65");
		String clientID = Config.getConfig().getLazadaClientID();
		String clientSecret = Config.getConfig().getLazadaClientSecret();
		callAPI("https://auth.lazada.com/rest", "/auth/token/refresh", null, map, "", "", "POST", clientID,
				clientSecret);
	}

	public static String callAPI(String url, String apiName, String accessToken, HashMap<String, String> map,
			String payload, String queryParams, String method, String clientID, String clientSecret) {
		String response = "";
		map.put("sign_method", signMethod);
		map.put("app_key", clientID);
		Long timestamp = System.currentTimeMillis();
		map.put("timestamp", "" + timestamp);
		String sign;
		try {
			sign = signApiRequest(apiName, map, null, clientSecret, signMethod);
			url = url + apiName;
			if (accessToken != null && apiName != "/auth/token/refresh") {
				url += "?access_token=" + accessToken + "&app_key=" + clientID;
			} else {
				url += "?app_key=" + clientID;
			}
			url += "&sign_method=hmac&sign=" + sign + "&timestamp=" + timestamp + queryParams;
			if (method.equals("POST")) {
				response = _doPost(url, payload, method);
			} else {
				response = _doGet(url, method);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}


	private static String _doPost(String url, String payload, String method) throws IOException {
		HttpURLConnection conn = null;
		OutputStream out = null;
		StringBuilder sb = new StringBuilder();
		InputStream inputStream = null;
		try {
			byte[] content = {};
			conn = getConnection(new URL(url), method);
			content = payload.getBytes("UTF-8");
			conn.setConnectTimeout(connectTimeout);
			conn.setReadTimeout(readTimeout);
			out = conn.getOutputStream();
			out.write(content);
			inputStream = conn.getInputStream();
			Reader inn = new BufferedReader(new InputStreamReader(new GZIPInputStream(inputStream), "UTF-8"));
			for (int c; (c = inn.read()) >= 0;)
				sb.append((char) c);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				out.close();
			}
			if (inputStream != null) {
				inputStream.close();
			}
			if (conn != null) {
				conn.disconnect();
			}
		}
		return sb.toString();
	}

	private static String _doGet(String url, String method) throws IOException {
		HttpURLConnection conn = null;
		OutputStream out = null;
		StringBuilder sb = new StringBuilder();
		InputStream inputStream = null;
		try {
			conn = getConnection(new URL(url), method);
			inputStream = conn.getInputStream();
			Reader inn = new BufferedReader(new InputStreamReader(new GZIPInputStream(inputStream), "UTF-8"));
			for (int c; (c = inn.read()) >= 0;)
				sb.append((char) c);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
			if (out != null) {
				out.close();
			}
			if (conn != null) {
				conn.disconnect();
			}
		}
		return sb.toString();
	}

	private static HttpURLConnection getConnection(URL url, String method) throws IOException {
		HttpURLConnection conn = null;

		conn = (HttpURLConnection) url.openConnection();

		if (conn instanceof HttpsURLConnection) {
			HttpsURLConnection connHttps = (HttpsURLConnection) conn;
			if (ignoreSSLCheck) {
				try {
					SSLContext ctx = SSLContext.getInstance("TLS");
					ctx.init(null, new TrustManager[] { new TrustAllTrustManager() }, new SecureRandom());
					connHttps.setSSLSocketFactory(ctx.getSocketFactory());
					connHttps.setHostnameVerifier(new HostnameVerifier() {
						public boolean verify(String hostname, SSLSession session) {
							return true;
						}
					});
				} catch (Exception e) {
					throw new IOException(e.toString());
				}
			} else {
				if (ignoreHostCheck) {
					connHttps.setHostnameVerifier(new HostnameVerifier() {
						public boolean verify(String hostname, SSLSession session) {
							return true;
						}
					});
				}
			}
			conn = connHttps;
		}

		conn.setRequestMethod(method);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("Host", "https://auth.lazada.com");
		conn.setRequestProperty("Accept", "text/xml,text/javascript");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
		conn.setRequestProperty("Accept-Encoding", "gzip");
		return conn;
	}

	public static String signApiRequest(String apiName, Map<String, String> params, String body, String appSecret,
			String signMethod) throws IOException {
		// first: sort all text parameters
		String[] keys = params.keySet().toArray(new String[0]);
		Arrays.sort(keys);
		// second: connect all text parameters with key and value
		StringBuilder query = new StringBuilder();
		query.append(apiName);

		for (String key : keys) {
			String value = params.get(key);
			if (areNotEmpty(key, value)) {
				query.append(key).append(value);
			}
		}

		// third：put the body to the end
		if (body != null) {
			query.append(body);
		}

		// next : sign the whole request
		byte[] bytes = null;

		bytes = encryptWithHmac(query.toString(), appSecret);

		// finally : transfer sign result from binary to upper hex string
		return byte2hex(bytes);
	}

	/**
	 * Check whether the given string list are null or blank.
	 */
	public static boolean areNotEmpty(String... values) {
		boolean result = true;
		if (values == null || values.length == 0) {
			result = false;
		} else {
			for (String value : values) {
				result &= !isEmpty(value);
			}
		}
		return result;
	}

	public static byte[] encryptWithHmac(String data, String secret) throws IOException {
		byte[] bytes = null;
		try {
			SecretKey secretKey = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacMD5");
			Mac mac = Mac.getInstance(secretKey.getAlgorithm());
			mac.init(secretKey);
			bytes = mac.doFinal(data.getBytes("UTF-8"));
		} catch (GeneralSecurityException gse) {
			throw new IOException(gse.toString());
		}
		return bytes;
	}

	/**
	 * Transfer binary array to HEX string.
	 */
	public static String byte2hex(byte[] bytes) {
		StringBuilder sign = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(bytes[i] & 0xFF);
			if (hex.length() == 1) {
				sign.append("0");
			}
			sign.append(hex.toUpperCase());
		}
		return sign.toString();
	}

	public static boolean isEmpty(String value) {
		int strLen;
		if (value == null || (strLen = value.length()) == 0) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if ((Character.isWhitespace(value.charAt(i)) == false)) {
				return false;
			}
		}
		return true;
	}

	public static class TrustAllTrustManager implements X509TrustManager {
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}
	}
}
