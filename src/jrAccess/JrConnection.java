package jrAccess;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.List;
import java.util.Map;

public class JrConnection extends URLConnection {

	private URLConnection mUrlConnection;
	private String[] mParams;
	private int resets = 0;
	
	protected JrConnection(URL url) throws IOException {
		super(url);
		setConnection(url);
	}

	public JrConnection(String... params) throws IOException {
		this(new URL(JrSession.accessDao.getJrUrl(params)));
		mParams = params;
	}
	
	public void setConnection(URL url) throws IOException {
		mUrlConnection = url.openConnection();
		mUrlConnection.setConnectTimeout(5000);
	}
	
	@Override
	public void connect() throws IOException {
		try {
			mUrlConnection.connect();
		} catch (IOException e) {
			resetConnection(e);
			this.connect();
		}
	}
	
	@Override
	public boolean getAllowUserInteraction() {
		return mUrlConnection.getAllowUserInteraction();
	}
	
	@Override
	public void addRequestProperty(String field, String newValue) {
		mUrlConnection.addRequestProperty(field, newValue);
	}
	
	@Override
	public int getConnectTimeout() {
		return mUrlConnection.getConnectTimeout();
	}
	
	@Override
	public Object getContent() throws IOException {
		try {
			return mUrlConnection.getContent();
		} catch (IOException e) {
			resetConnection(e);
			return this.getContent();
		}
	}
	
	@Override
	public Object getContent(Class[] types) throws IOException {
		try {
			return mUrlConnection.getContent(types);
		} catch (IOException e) {
			resetConnection(e);
			return this.getContent(types);
		}
	}
	
	@Override
	public String getContentEncoding() {
		return mUrlConnection.getContentEncoding();
	}
	
	@Override
	public int getContentLength() {
		return mUrlConnection.getContentLength();
	}
	
	@Override
	public String getContentType() {
		return mUrlConnection.getContentType();
	}
	
	@Override
	public long getDate() {
		return mUrlConnection.getDate();
	}
	
	@Override
	public boolean getDefaultUseCaches() {
		return mUrlConnection.getDefaultUseCaches();
	}
	
	@Override
	public boolean getDoInput() {
		return mUrlConnection.getDoInput();
	}
	
	@Override
	public boolean getDoOutput() {
		return mUrlConnection.getDoOutput();
	}
	
	@Override
	public long getExpiration() {
		return mUrlConnection.getExpiration();
	}
	
	@Override
	public String getHeaderField(int pos) {
		return mUrlConnection.getHeaderField(pos);
	}
	
	@Override
	public String getHeaderField(String key) {
		return mUrlConnection.getHeaderField(key);
	}
	
	@Override
	public long getHeaderFieldDate(String field, long defaultValue) {
		return mUrlConnection.getHeaderFieldDate(field, defaultValue);
	}
	
	@Override
	public int getHeaderFieldInt(String field, int defaultValue) {
		return mUrlConnection.getHeaderFieldInt(field, defaultValue);
	}
	
	@Override
	public String getHeaderFieldKey(int posn) {
		return mUrlConnection.getHeaderFieldKey(posn);
	}
	
	@Override
	public Map<String, List<String>> getHeaderFields() {
		return mUrlConnection.getHeaderFields();
	}
	
	@Override
	public long getIfModifiedSince() {
		return mUrlConnection.getIfModifiedSince();
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		try {
			return mUrlConnection.getInputStream();
		} catch (IOException e) {
			resetConnection(e);
			return this.getInputStream();
		}
	}
	
	@Override
	public long getLastModified() {
		return mUrlConnection.getLastModified();
	}
	
	@Override
	public OutputStream getOutputStream() throws IOException {
		return mUrlConnection.getOutputStream();
		
	}
	
	@Override
	public Permission getPermission() throws IOException {
		return mUrlConnection.getPermission();
	}
	
	@Override
	public int getReadTimeout() {
		return mUrlConnection.getReadTimeout();
	}
	
	@Override
	public Map<String, List<String>> getRequestProperties() {
		return mUrlConnection.getRequestProperties();
	}
	
	@Override
	public String getRequestProperty(String field) {
		return mUrlConnection.getRequestProperty(field);
	}
	
	@Override
	public URL getURL() {
		return mUrlConnection.getURL();
	}
	
	@Override
	public boolean getUseCaches() {
		return mUrlConnection.getUseCaches();
	}
	
	@Override
	public void setAllowUserInteraction(boolean newValue) {
		mUrlConnection.setAllowUserInteraction(newValue);
	}
	
	@Override
	public void setConnectTimeout(int timeoutMillis) {
		mUrlConnection.setConnectTimeout(timeoutMillis);
	}
	
	@Override
	public void setDefaultUseCaches(boolean newValue) {
		mUrlConnection.setDefaultUseCaches(newValue);
	}
	
	@Override
	public void setDoInput(boolean newValue) {
		mUrlConnection.setDoInput(newValue);
	}
	
	@Override
	public void setDoOutput(boolean newValue) {
		mUrlConnection.setDoOutput(newValue);
	}
	
	@Override
	public void setIfModifiedSince(long newValue) {
		mUrlConnection.setIfModifiedSince(newValue);
	}
	
	@Override
	public void setReadTimeout(int timeoutMillis) {
		mUrlConnection.setReadTimeout(timeoutMillis);
	}
	
	@Override
	public void setRequestProperty(String field, String newValue) {
		mUrlConnection.setRequestProperty(field, newValue);
	}
	
	@Override
	public void setUseCaches(boolean newValue) {
		mUrlConnection.setUseCaches(newValue);
	}
	
	@Override
	public String toString() {
		
		return mUrlConnection.toString();
	}
	
	private void resetConnection(IOException ioEx) throws IOException {
		if (resets > 6) throw ioEx;
		resets++;
		JrSession.accessDao.resetUrl();
		String url = JrSession.accessDao.getJrUrl(mParams);
		if (url == null || url.isEmpty()) throw ioEx;
		setConnection(new URL(url));
	}
}
