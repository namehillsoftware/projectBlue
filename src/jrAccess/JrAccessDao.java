package jrAccess;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class JrAccessDao {
	private String mToken;
	private boolean status;
	private volatile String mActiveUrl = "";
	private String remoteIp;
	private int port;
	private List<String> localIps = new ArrayList<String>();
	private List<String> macAddresses = new ArrayList<String>();
	private int urlIndex = -1;
	
	public JrAccessDao(String status) {
		this.status = status != null && status.equalsIgnoreCase("OK");
	}

	/**
	 * @return the status
	 */
	public boolean isStatus() {
		return status;
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
	
	public void resetUrl() {
		mActiveUrl = "";
		clearToken();
	}
	
	public String getActiveUrl() {
		if (!mActiveUrl.isEmpty()) {
			try {
				if (testConnection(mActiveUrl)) return mActiveUrl;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (!JrSession.IsLocalOnly) {
			try {
				mActiveUrl = getRemoteUrl();
	        	if (testConnection(getRemoteUrl())) return mActiveUrl;
			} catch (Exception e) {
				mActiveUrl = "";
				e.printStackTrace();
			}
		} else { 
			for (urlIndex = 0; urlIndex < localIps.size(); urlIndex++) {
				try {
					mActiveUrl = getLocalIpUrl(urlIndex);
		        	if (testConnection(mActiveUrl)) return mActiveUrl;
				} catch (Exception e) {
					mActiveUrl = "";
					e.printStackTrace();
				}
			}
		}

		
		mActiveUrl = "";
		return mActiveUrl;
	}
	
	public String getJrUrl(String... params) {
		// Add base url
		String url = getActiveUrl();
		// Add action
		url += params[0];
		
		url += "?";
		// Add token
		if (mToken != null)
			url += "Token=" + getToken() + "&";
		
		// add arguments
		if (params.length > 1) {
			for (int i = 1; i < params.length; i++) {
				String[] keyValue = params[i].split("=");
				try {
					url += URLEncoder.encode(keyValue[0], "UTF-8").replace("+", "%20");
				} catch (UnsupportedEncodingException e) {
					url += keyValue[0];
				}
				if (keyValue.length > 1) {
					url += "=";
					try {
						url += URLEncoder.encode(keyValue[1], "UTF-8").replace("+", "%20");
					} catch (UnsupportedEncodingException e) {
						url += keyValue[1];
					}
				}
				url += "&";
			}
		}
		
		return url;
	}
	
	private String getToken(String url) {
		if (!url.equals(mActiveUrl) || mToken == null || mToken.isEmpty()) {
			try {
				FutureTask<String> getTokenTask = new FutureTask<String>(new GetAuthToken(url));
				Thread getTokenThread = new Thread(getTokenTask);
				getTokenThread.setName("Auth Token Retrieval Thread");
				getTokenThread.start();
				mToken = getTokenTask.get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return mToken;
	}
	
	private String getToken() {
		return getToken(getActiveUrl());
	}
	
	private void clearToken() {
		mToken = null;
	}
	
	private boolean testConnection(String url) {
		return getToken(url) != null;
	}
	
	private class GetAuthToken implements Callable<String> {
		
		private String mUrl;
		
		public GetAuthToken(String url) {
			mUrl = url;
		}
		
		@Override
		public String call() throws InterruptedException, ExecutionException {
			// Get authentication token
			String token = null;
			try {
				URLConnection authConn = (new URL(mUrl + "Authenticate")).openConnection();
				authConn.setReadTimeout(5000);
				authConn.setConnectTimeout(5000);
				if (!JrSession.UserAuthCode.isEmpty())
					authConn.setRequestProperty("Authorization", "basic " + JrSession.UserAuthCode);
				
		    	JrResponse response = JrResponse.fromInputStream(authConn.getInputStream());
		    	if (response != null && response.items.containsKey("Token"))
		    		token = response.items.get("Token");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return token;
		}
	}
}
