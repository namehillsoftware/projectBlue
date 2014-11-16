package com.lasthopesoftware.bluewater.data.service.access.connection;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xmlwise.XmlElement;
import xmlwise.Xmlwise;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.StandardRequest;
import com.lasthopesoftware.bluewater.data.service.objects.AccessConfiguration;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class ConnectionManager {
	private static AccessConfiguration mAccessConfiguration;
	private static String mAccessString = null;
	private static String mAuthCode = null;
	
	private static final int stdTimeoutTime = 30000;
	private static final Logger mLogger = LoggerFactory.getLogger(ConnectionManager.class);
	
	private static CopyOnWriteArrayList<OnAccessStateChange> mOnAccessStateChangeListeners = new CopyOnWriteArrayList<OnAccessStateChange>();
	
	private static Object syncObj = new Object();
	
	public static void buildConfiguration(Context context, String accessString, OnCompleteListener<Integer, Void, Boolean> onBuildComplete) {
		buildConfiguration(context, accessString, stdTimeoutTime, onBuildComplete);
	}
	
	public static void buildConfiguration(Context context, String accessString, int timeout, OnCompleteListener<Integer, Void, Boolean> onBuildComplete) {
		buildConfiguration(context, accessString, null, timeout, onBuildComplete);
	}
	
	public static void buildConfiguration(Context context, String accessString, String authCode, OnCompleteListener<Integer, Void, Boolean> onBuildComplete) {
		buildConfiguration(context, accessString, authCode, stdTimeoutTime, onBuildComplete);
	}
	
	public static void buildConfiguration(Context context, String accessString, String authCode, int timeout, final OnCompleteListener<Integer, Void, Boolean> onBuildComplete) throws NullPointerException {
		if (accessString == null)
			throw new NullPointerException("The access string cannot be null.");
		
		mAccessString = accessString;
		synchronized(syncObj) {
			mAuthCode = authCode;
			if (timeout <= 0) timeout = stdTimeoutTime;
			try {
				buildAccessConfiguration(mAccessString, timeout, new OnCompleteListener<String, Void, AccessConfiguration>() {
					
					@Override
					public void onComplete(ISimpleTask<String, Void, AccessConfiguration> owner, AccessConfiguration result) {
						synchronized(syncObj) {
							mAccessConfiguration = result;
						}
						
						if (mAccessConfiguration == null) {
							final SimpleTask<Integer, Void, Boolean> pseudoTask = new SimpleTask<Integer, Void, Boolean>(new OnExecuteListener<Integer, Void, Boolean>() {
	
								@Override
								public Boolean onExecute(ISimpleTask<Integer, Void, Boolean> owner, Integer... params) throws Exception {
									return Boolean.FALSE;
								}
								
							}); 
							pseudoTask.addOnCompleteListener(onBuildComplete);
							pseudoTask.execute(AsyncTask.THREAD_POOL_EXECUTOR);
							return;
						}
	
						ConnectionTester.doTest(onBuildComplete);
					}
				});
			} catch (NullPointerException ne) {
				throw ne;
			}
		}
	}
	
	public static void refreshConfiguration(Context context, OnCompleteListener<Integer, Void, Boolean> onRefreshComplete) throws NullPointerException  {
		refreshConfiguration(context, -1, onRefreshComplete);
	}
	
	public static void refreshConfiguration(final Context context, final int timeout, final OnCompleteListener<Integer, Void, Boolean> onRefreshComplete) throws NullPointerException  {
		if (mAccessConfiguration == null) {
			if (mAccessString == null || mAccessString.isEmpty())
				throw new NullPointerException("The static access string has been lost. Please reset the connection session.");
			
			buildConfiguration(context, mAccessString, mAuthCode, timeout, onRefreshComplete);
			return;
		}
		
		final OnCompleteListener<Integer, Void, Boolean> mTestConnectionCompleteListener = new OnCompleteListener<Integer, Void, Boolean>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Boolean> owner, Boolean result) {
				if (result == Boolean.TRUE) {
					onRefreshComplete.onComplete(owner, result);
					return;
				}
				
				buildConfiguration(context, mAccessString, mAuthCode, timeout, onRefreshComplete);
			}
		
		};
		
		
		if (timeout > 0)
			ConnectionTester.doTest(timeout, mTestConnectionCompleteListener);
		else
			ConnectionTester.doTest(mTestConnectionCompleteListener);
	}	
	
	public static MediaCenterConnection getConnection(String... params) throws IOException {
		synchronized(syncObj) {
			if (mAccessConfiguration == null) return null;
			URL url = new URL(mAccessConfiguration.buildMediaCenterUrl(params));
			return mAuthCode == null || mAuthCode.isEmpty() ? new MediaCenterConnection(url) : new MediaCenterConnection(url, mAuthCode);
		}
	}
	
	public static String getFormattedUrl(String... params) {
		synchronized(syncObj) {
			if (mAccessConfiguration == null) return null;
			return mAccessConfiguration.buildMediaCenterUrl(params);
		}
	}
	
	private static void buildAccessConfiguration(String accessString, int timeout, OnCompleteListener<String, Void, AccessConfiguration> onGetAccessComplete) throws NullPointerException {
		if (accessString == null)
			throw new NullPointerException("The access string cannot be null");
		
		for (OnAccessStateChange onAccessStateChange : mOnAccessStateChangeListeners)
			onAccessStateChange.gettingUri(accessString);
		
		final int _timeout = timeout;
		
		final SimpleTask<String, Void, AccessConfiguration> mediaCenterAccessTask = new SimpleTask<String, Void, AccessConfiguration>(new OnExecuteListener<String, Void, AccessConfiguration>() {
			
			@Override
			public AccessConfiguration onExecute(ISimpleTask<String, Void, AccessConfiguration> owner, String... params) throws Exception {
				try {
					final AccessConfiguration accessDao = new AccessConfiguration();
					String accessString = params[0];
					if (accessString.contains(".")) {
						if (!accessString.contains(":")) accessString += ":80";
						if (!accessString.startsWith("http://")) accessString = "http://" + accessString;
					}
					
					if (UrlValidator.getInstance().isValid(accessString)) {
						final Uri jrUrl = Uri.parse(accessString);
						accessDao.setRemoteIp(jrUrl.getHost());
						accessDao.setPort(jrUrl.getPort());
						accessDao.setStatus(true);
					} else {
						final HttpURLConnection conn = (HttpURLConnection)(new URL("http://webplay.jriver.com/libraryserver/lookup?id=" + accessString)).openConnection();
						
						conn.setConnectTimeout(_timeout);
						try {
							final InputStream is = conn.getInputStream();
							try {
								final XmlElement xml = Xmlwise.createXml(IOUtils.toString(is));
								
								accessDao.setStatus(xml.getAttribute("Status").equalsIgnoreCase("OK"));
								accessDao.setPort(Integer.parseInt(xml.getUnique("port").getValue()));
								accessDao.setRemoteIp(xml.getUnique("ip").getValue());
								for (String localIp : xml.getUnique("localiplist").getValue().split(","))
									accessDao.getLocalIps().add(localIp);
								for (String macAddress : xml.getUnique("macaddresslist").getValue().split(","))
									accessDao.getMacAddresses().add(macAddress);
							} finally {
								is.close();
							}
						} finally {
							conn.disconnect();
						}
					}
					return accessDao;
				} catch (ClientProtocolException c) {
					mLogger.error(c.getMessage());
				} catch (IOException i) {
					mLogger.error(i.getMessage());
				} catch (Exception e) {
					mLogger.warn(e.toString());
				}
				
				return null;
			}
		});
		
		if (onGetAccessComplete != null)
			mediaCenterAccessTask.addOnCompleteListener(onGetAccessComplete);
		
		mediaCenterAccessTask.execute(AsyncTask.THREAD_POOL_EXECUTOR, accessString);
	}
		
	public static class MediaCenterConnection extends HttpURLConnection {
	
		private HttpURLConnection mHttpConnection;
		
		private MediaCenterConnection(URL url) throws IOException {
			super(url);
			setConnection(url);
		}
		
		private MediaCenterConnection(URL url, String authCode) throws IOException {
			this(url);
			try {
				mHttpConnection.setRequestProperty("Authorization", "basic " + authCode);
			} catch (Exception e) {
				LoggerFactory.getLogger(MediaCenterConnection.class).error(e.toString(), e);
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
			} catch (IOException e) {
				throw e;
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
	
	private static class ConnectionTester {
		
		private static final Logger mLogger = LoggerFactory.getLogger(ConnectionTester.class); 
		
		public static void doTest(OnCompleteListener<Integer, Void, Boolean> onTestComplete) {
			doTest(stdTimeoutTime, onTestComplete);
		}
		
		public static void doTest(int timeout, OnCompleteListener<Integer, Void, Boolean> onTestComplete) {
			final SimpleTask<Integer, Void, Boolean> connectionTestTask = new SimpleTask<Integer, Void, Boolean>(new OnExecuteListener<Integer, Void, Boolean>() {

				@Override
				public Boolean onExecute(ISimpleTask<Integer, Void, Boolean> owner, Integer... params) throws Exception {
					Boolean result = Boolean.FALSE;
					
					final HttpURLConnection conn = getConnection("Alive");
					try {
				    	conn.setConnectTimeout(params[0]);
				    	final InputStream is = conn.getInputStream();
				    	try {
							final StandardRequest responseDao = StandardRequest.fromInputStream(is);
					    	
					    	result = Boolean.valueOf(responseDao != null && responseDao.isStatus());
				    	} finally {
				    		is.close();
				    	}
					} catch (MalformedURLException m) {
						mLogger.warn(m.getMessage());
					} catch (FileNotFoundException f) {
						mLogger.warn(f.getMessage());
					} catch (IOException e) {
						mLogger.warn(e.getMessage());
					} catch (IllegalArgumentException i) {
						mLogger.warn(i.getMessage());
					} finally {
						conn.disconnect();
					}
					
					return result;
				}
				
			});
			
			if (onTestComplete != null)
				connectionTestTask.addOnCompleteListener(onTestComplete);
			
			connectionTestTask.execute(AsyncTask.THREAD_POOL_EXECUTOR, timeout);
		}		
	}
	
	public interface OnAccessStateChange {
		public void gettingUri(String accessString);
		public void establishingConnection(Uri destinationUri);
		public void establishingConnectionCompleted(Uri destinationUri);
	}
}
