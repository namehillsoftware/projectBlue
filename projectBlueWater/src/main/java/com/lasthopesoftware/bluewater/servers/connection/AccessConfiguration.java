package com.lasthopesoftware.bluewater.servers.connection;

import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class AccessConfiguration {
	private boolean status;
	private volatile String mActiveUrl = "";
	private String remoteIp;
	private int port;
	private List<String> localIps = new ArrayList<>();
	private List<String> macAddresses = new ArrayList<>();
	private int urlIndex = -1;
	private boolean isLocalOnly;
	
	public AccessConfiguration() {
	}

	/**
	 * @return the status
	 */
	public boolean isStatus() {
		return status;
	}
	
	public void setStatus(boolean status) {
		this.status = status;
	}
	
	/**
	 * @return the remoteIp
	 */
	public String getRemoteIp() {
		return remoteIp;
	}
	/**
	 * @param remoteIp the remoteIp to set
	 */
	public void setRemoteIp(String remoteIp) {
		this.remoteIp = remoteIp;
	}
	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}
	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}
	/**
	 * @return the localIps
	 */
	public List<String> getLocalIps() {
		return localIps;
	}
	
	public boolean isLocalOnly() {
		return isLocalOnly;
	}

	public void setLocalOnly(boolean isLocalOnly) {
		this.isLocalOnly = isLocalOnly;
	}

	/**
	 * @return the macAddresses
	 */
	public List<String> getMacAddresses() {
		return macAddresses;
	}
	
	private String getRemoteUrl() {
		return "http://" + remoteIp  + ":" + String.valueOf(port) + "/MCWS/v1/";
	}
	
	private String getLocalIpUrl(int index) {
		return "http://" + localIps.get(index) + ":" + String.valueOf(port) + "/MCWS/v1/";
	}

	public String getActiveUrl() {
		if (!mActiveUrl.isEmpty()) {
			try {
				/*if (testConnection(mActiveUrl))*/ return mActiveUrl;
			} catch (Exception e) {
				LoggerFactory.getLogger(AccessConfiguration.class).error(e.toString(), e);
			}
		}
		
		try {
			if (!isLocalOnly) {
				try {
					mActiveUrl = getRemoteUrl();
			    	/*if (testConnection(getRemoteUrl()))*/ return mActiveUrl;
				} catch (Exception e) {
					mActiveUrl = "";
					LoggerFactory.getLogger(AccessConfiguration.class).error(e.toString(), e);
				}
			} else { 
				for (urlIndex = 0; urlIndex < localIps.size(); urlIndex++) {
					try {
						mActiveUrl = getLocalIpUrl(urlIndex);
			        	/*if (testConnection(mActiveUrl))*/ return mActiveUrl;
					} catch (Exception e) {
						mActiveUrl = "";
						LoggerFactory.getLogger(AccessConfiguration.class).error(e.toString(), e);
					}
				}
			}
		} catch (Exception e) {
			mActiveUrl = "";
			LoggerFactory.getLogger(AccessConfiguration.class).error(e.toString(), e);
		}
		
		mActiveUrl = "";
		return mActiveUrl;
	}
	
	public String buildMediaCenterUrl(String... params) {
		// Add base url
		final String url = getActiveUrl();
		if (url == null || url.isEmpty()) return null; 
		
		if (params.length == 0) return url;
		
		final StringBuilder urlBuilder = new StringBuilder(url);
		
		// Add action
		urlBuilder.append(params[0]);
		
		// add arguments
		if (params.length > 1) {
			for (int i = 1; i < params.length; i++) {
				urlBuilder.append(i == 1 ? '?' : '&');
				
				final String param = params[i];
				
				final int equalityIndex = param.indexOf('=');
				if (equalityIndex < 0) {
					urlBuilder.append(encodeParameter(param));
					continue;
				}
				
				urlBuilder.append(encodeParameter(param.substring(0, equalityIndex))).append('=').append(encodeParameter(param.substring(equalityIndex + 1)));
			}
		}
		
		return urlBuilder.toString();
	}
	
	private static String encodeParameter(String parameter) {
		try {
			return URLEncoder.encode(parameter, "UTF-8").replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			return parameter;
		}
	}
	
}
