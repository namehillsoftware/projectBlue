package com.lasthopesoftware.bluewater.data.service.access.connection;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.LoggerFactory;

import xmlwise.XmlElement;
import xmlwise.Xmlwise;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.JrAccessDao;
import com.lasthopesoftware.bluewater.data.session.JrSession;

public class JrConnectionManager {
	private static JrAccessDao mAccessConfiguration;
	private static String mAccessCode;
	private static String mAuthCode;
	
	private static Object syncObj = new Object();
	
	public static boolean buildConfiguration(Context context, String accessCode, String authCode) {
		synchronized(syncObj) {
			mAuthCode = authCode;
		}
		return buildConfiguration(context, accessCode);
	}
	
	public static boolean buildConfiguration(Context context, String accessCode) {
		mAccessCode = accessCode;
		synchronized(syncObj) {
			mAccessConfiguration = buildAccessConfiguration(mAccessCode);
		}
		return mAccessConfiguration != null && JrTestConnection.doTest(context);
	}
	
	public static boolean updateConfiguration(Context context) {
		if (!JrTestConnection.doTest(context))
			return buildConfiguration(context, mAccessCode);
		return false;
	}

	private static JrAccessDao buildAccessConfiguration(String accessCode) {
		try {
			JrAccessDao access = new GetMcAccess().execute(accessCode).get();
			if (access.getActiveUrl() != null && !access.getActiveUrl().isEmpty())
				return access;
		} catch (InterruptedException e) {
			LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
		} catch (ExecutionException e) {
			LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
		}

		return null;
	}
	
	private static class GetMcAccess extends AsyncTask<String, Void, JrAccessDao> {

		@Override
		protected JrAccessDao doInBackground(String... params) {

			JrAccessDao accessDao = null;
			try {
				accessDao = new JrAccessDao();
				
				if (UrlValidator.getInstance().isValid(params[0])) {
					Uri jrUrl = Uri.parse(params[0]);
					accessDao.setRemoteIp(jrUrl.getHost());
					accessDao.setPort(jrUrl.getPort());
					accessDao.setStatus(true);
				} else {
					URLConnection conn = (new URL("http://webplay.jriver.com/libraryserver/lookup?id=" + params[0])).openConnection();
					XmlElement xml = Xmlwise.createXml(IOUtils.toString(conn.getInputStream()));
					
					
					accessDao.setStatus(xml.getAttribute("Status").equalsIgnoreCase("OK"));
					accessDao.setPort(Integer.parseInt(xml.getUnique("port").getValue()));
					accessDao.setRemoteIp(xml.getUnique("ip").getValue());
					for (String localIp : xml.getUnique("localiplist").getValue().split(","))
						accessDao.getLocalIps().add(localIp);
					for (String macAddress : xml.getUnique("macaddresslist").getValue().split(","))
						accessDao.getMacAddresses().add(macAddress);
				}
			} catch (ClientProtocolException e) {
				LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
			} catch (IOException e) {
				LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
			} catch (Exception e) {
				LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
			}

			return accessDao;
		}
	}
	
	public static HttpURLConnection getConnection(String... params) throws IOException {
		synchronized(syncObj) {
			if (mAccessConfiguration == null) return null;
			URL url = new URL(mAccessConfiguration.getJrUrl(params));
			return mAuthCode == null ? new JrConnection(url) : new JrConnection(url, mAuthCode);
		}
	}
	
	private static class JrConnection extends HttpURLConnection {
	
		private HttpURLConnection mHttpConnection;
	//	private String[] mParams;
	//	private int resets = 0, maxResets = -1;
	//	private static final String failedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\r\n<Response Status=\"Failure\"/>\r\n";
	//	private InputStream mInputStream;
	//	private boolean mIsFound;
		
		public JrConnection(URL url) throws IOException {
			super(url);
			setConnection(url);
		}
		
		public JrConnection(URL url, String authCode) throws IOException {
			this(url);
			try {
				mHttpConnection.setRequestProperty("Authorization", "basic " + authCode);
			} catch (Exception e) {
				LoggerFactory.getLogger(JrConnection.class).error(e.toString(), e);
			}
		}
		
		public void setConnection(URL url) throws IOException {
			mHttpConnection = (HttpURLConnection)url.openConnection();
			mHttpConnection.setConnectTimeout(5000);
			mHttpConnection.setReadTimeout(180000);
		}
		
		@Override
		public void connect() throws IOException {
			try {
				mHttpConnection.connect();
			} catch (IOException e) {
				resetConnection(e);
				this.connect();
			}
		}
		
		@Override
		public boolean getAllowUserInteraction() {
			return mHttpConnection.getAllowUserInteraction();
		}
		
		@Override
		public void addRequestProperty(String field, String newValue) {
			mHttpConnection.addRequestProperty(field, newValue);
		}
		
		@Override
		public int getConnectTimeout() {
			return mHttpConnection.getConnectTimeout();
		}
		
		@Override
		public Object getContent() throws IOException {
			try {
				return mHttpConnection.getContent();
			} catch (IOException e) {
				resetConnection(e);
				return this.getContent();
			}
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public Object getContent(Class[] types) throws IOException {
			try {
				return mHttpConnection.getContent(types);
			} catch (IOException e) {
				resetConnection(e);
				return this.getContent(types);
			}
		}
		
		@Override
		public String getContentEncoding() {
			return mHttpConnection.getContentEncoding();
		}
		
		@Override
		public int getContentLength() {
			return mHttpConnection.getContentLength();
		}
		
		@Override
		public String getContentType() {
			return mHttpConnection.getContentType();
		}
		
		@Override
		public long getDate() {
			return mHttpConnection.getDate();
		}
		
		@Override
		public boolean getDefaultUseCaches() {
			return mHttpConnection.getDefaultUseCaches();
		}
		
		@Override
		public boolean getDoInput() {
			return mHttpConnection.getDoInput();
		}
		
		@Override
		public boolean getDoOutput() {
			return mHttpConnection.getDoOutput();
		}
		
		@Override
		public long getExpiration() {
			return mHttpConnection.getExpiration();
		}
		
		@Override
		public String getHeaderField(int pos) {
			return mHttpConnection.getHeaderField(pos);
		}
		
		@Override
		public String getHeaderField(String key) {
			return mHttpConnection.getHeaderField(key);
		}
		
		@Override
		public long getHeaderFieldDate(String field, long defaultValue) {
			return mHttpConnection.getHeaderFieldDate(field, defaultValue);
		}
		
		@Override
		public int getHeaderFieldInt(String field, int defaultValue) {
			return mHttpConnection.getHeaderFieldInt(field, defaultValue);
		}
		
		@Override
		public String getHeaderFieldKey(int posn) {
			return mHttpConnection.getHeaderFieldKey(posn);
		}
		
		@Override
		public Map<String, List<String>> getHeaderFields() {
			return mHttpConnection.getHeaderFields();
		}
		
		@Override
		public long getIfModifiedSince() {
			return mHttpConnection.getIfModifiedSince();
		}
		
		@Override
		public InputStream getInputStream() throws IOException {
			try {
				return mHttpConnection.getInputStream();
			} catch (FileNotFoundException fe) {
				throw fe;
			}
			catch (IOException e) {
				resetConnection(e);
				return this.getInputStream();
			}
		}
		
		@Override
		public long getLastModified() {
			return mHttpConnection.getLastModified();
		}
		
		@Override
		public OutputStream getOutputStream() throws IOException {
			return mHttpConnection.getOutputStream();
			
		}
		
		@Override
		public Permission getPermission() throws IOException {
			return mHttpConnection.getPermission();
		}
		
		@Override
		public int getReadTimeout() {
			return mHttpConnection.getReadTimeout();
		}
		
		@Override
		public Map<String, List<String>> getRequestProperties() {
			return mHttpConnection.getRequestProperties();
		}
		
		@Override
		public String getRequestProperty(String field) {
			return mHttpConnection.getRequestProperty(field);
		}
		
		@Override
		public URL getURL() {
			return mHttpConnection.getURL();
		}
		
		@Override
		public boolean getUseCaches() {
			return mHttpConnection.getUseCaches();
		}
		
		@Override
		public void setAllowUserInteraction(boolean newValue) {
			mHttpConnection.setAllowUserInteraction(newValue);
		}
		
		@Override
		public void setConnectTimeout(int timeoutMillis) {
			mHttpConnection.setConnectTimeout(timeoutMillis);
		}
		
		@Override
		public void setDefaultUseCaches(boolean newValue) {
			mHttpConnection.setDefaultUseCaches(newValue);
		}
		
		@Override
		public void setDoInput(boolean newValue) {
			mHttpConnection.setDoInput(newValue);
		}
		
		@Override
		public void setDoOutput(boolean newValue) {
			mHttpConnection.setDoOutput(newValue);
		}
		
		@Override
		public void setIfModifiedSince(long newValue) {
			mHttpConnection.setIfModifiedSince(newValue);
		}
		
		@Override
		public void setReadTimeout(int timeoutMillis) {
			mHttpConnection.setReadTimeout(timeoutMillis);
		}
		
		@Override
		public void setRequestProperty(String field, String newValue) {
			mHttpConnection.setRequestProperty(field, newValue);
		}
		
		@Override
		public void setUseCaches(boolean newValue) {
			mHttpConnection.setUseCaches(newValue);
		}
		
		@Override
		public String toString() {
			
			return mHttpConnection.toString();
		}
		
		private void resetConnection(IOException ioEx) throws IOException {
			throw ioEx;
		}
	
		@Override
		public void disconnect() {
			mHttpConnection.disconnect();
		}
	
		@Override
		public boolean usingProxy() {
			return mHttpConnection.usingProxy();
		}
	}
}