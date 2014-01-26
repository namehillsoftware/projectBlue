package com.lasthopesoftware.bluewater.data.service.access.connection;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Map;

import com.lasthopesoftware.bluewater.data.session.JrSession;

public class JrConnection extends HttpURLConnection {

	private HttpURLConnection mHttpConnection;
//	private String[] mParams;
//	private int resets = 0, maxResets = -1;
//	private static final String failedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\r\n<Response Status=\"Failure\"/>\r\n";
//	private InputStream mInputStream;
//	private boolean mIsFound;
	
	protected JrConnection(URL url) throws IOException {
		super(url);
		setConnection(url);
	}

	public JrConnection(String... params) throws IOException {
		this(new URL(JrSession.accessDao.getJrUrl(params)));
	}
	
	public void setConnection(URL url) throws IOException {
		mHttpConnection = (HttpURLConnection)url.openConnection();
		mHttpConnection.setConnectTimeout(5000);
		mHttpConnection.setReadTimeout(180000);
		try {
			if (!JrSession.GetLibrary().getAuthKey().isEmpty())
				mHttpConnection.setRequestProperty("Authorization", "basic " + JrSession.GetLibrary().getAuthKey());
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		/*if (resets > maxResets)*/ throw ioEx;
//		resets++;
//		JrSession.accessDao.resetUrl();
//		String url = JrSession.accessDao.getJrUrl(mParams);
//		if (url == null || url.isEmpty()) throw ioEx;
//		setConnection(new URL(url));
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
