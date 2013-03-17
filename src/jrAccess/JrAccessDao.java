package jrAccess;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.zip.Checksum;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.os.AsyncTask;

public class JrAccessDao {
	private String mToken;
	private boolean status;
	private volatile String mActiveUrl = "";
	private String remoteIp;
	private int port;
	private List<String> localIps = new ArrayList<String>();
	private List<String> macAddresses = new ArrayList<String>();
	private int urlIndex = -1;
	private long nextConnectionCheck;
	private boolean mConnectionStatus = false;
	
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
	
	public String getActiveUrl() {
		if (!mActiveUrl.isEmpty()) {
			try {
				if (testConnection(mActiveUrl)) return mActiveUrl;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
			
		
		for (urlIndex = 0; urlIndex < localIps.size() - 1; urlIndex++) {
			try {
				mActiveUrl = getLocalIpUrl(urlIndex);
	        	if (testConnection(mActiveUrl)) return mActiveUrl;
			} catch (Exception e) {
				mActiveUrl = "";
				e.printStackTrace();
			}
		}

		try {
			mActiveUrl = getRemoteUrl();
        	if (testConnection(getRemoteUrl())) return mActiveUrl;
		} catch (Exception e) {
			mActiveUrl = "";
			e.printStackTrace();
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
				url += params[i] + "&";
			}
		}
		
		return url;
	}
	
	public String getToken(String url) {
		if (!url.equals(mActiveUrl) || mToken == null || mToken.isEmpty()) {
			try {
				mToken = new GetAuthToken().execute(url).get();
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
	
	public String getToken() {
		if (mToken == null || mToken.isEmpty()) {
			try {
				mToken = new GetAuthToken().execute(getActiveUrl()).get();
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
	
	private boolean testConnection(String url) throws InterruptedException, ExecutionException {
		if (getToken(url) == null) return false;
		long currentTime = new Date().getTime();
		if (currentTime > (nextConnectionCheck)) { 
			FutureTask<Boolean> statusTask = new FutureTask<Boolean>(new JrTestXmlResponse(url));
			Thread getStatusThread = new Thread(statusTask);
			getStatusThread.start();
			mConnectionStatus = statusTask.get().booleanValue();
			nextConnectionCheck = new Date().getTime() + 1000;
		}
		return mConnectionStatus;
	}
	
	private class JrTestXmlResponse implements Callable<Boolean> {
		private String mUrl;
		public JrTestXmlResponse(String url) {
			mUrl = url;
		}
		
		@Override
		public Boolean call() throws Exception {
			Boolean result = Boolean.FALSE;
			
			// Add base url
			String url = mUrl + "Alive?";
			url += "Token=" + getToken();
			
			URLConnection conn;
			try {
				conn = (new URL(url)).openConnection();
				conn.setConnectTimeout(5000);
		    	
				JrResponse responseDao = JrResponse.fromInputStream(conn.getInputStream());
		    	
		    	result = responseDao != null && responseDao.isStatus() ? Boolean.TRUE : Boolean.FALSE;
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return result;
		}
	}
	
	private class GetAuthToken extends AsyncTask<String, Void, String> {
		
		@Override
		protected String doInBackground(String... params) {
			// Get authentication token
			String token = null;
			try {
				URLConnection authConn = (new URL(params[0] + "Authenticate")).openConnection();
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
