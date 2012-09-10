package jrAccess;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.os.AsyncTask;

public class JrAccessDao {
	private String mToken;
	private boolean status;
	private String validUrl = "";
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
	
	public String getValidUrl() {

		if (validUrl.equals("")) {
			for (urlIndex = 0; urlIndex < localIps.size() - 1; urlIndex++) {
				try {
					validUrl = getLocalIpUrl(urlIndex);
		        	if (testConnection(validUrl))
		        		break;
		        	
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				validUrl = "";
			}
			
			if (validUrl.equals("")) {
				try {
		        	if (testConnection(getRemoteUrl()))
		        		validUrl = getRemoteUrl();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return validUrl;
	}
	
	public String getToken() {
		if (mToken == null || mToken.isEmpty()) {
			try {
				mToken = new GetAuthToken().execute(getValidUrl()).get();
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
		JrResponseDao response = new GetJrResponse().execute(new String[] { url, "Alive" }).get();
		return response != null && response.isStatus();
	}
	
	public class GetAuthToken extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			// Get authentication token
			String token = null;
			try {
				URLConnection authConn = (new URL(params[0] + "Authenticate")).openConnection();
				authConn.setReadTimeout(5000);
				if (!JrSession.UserAuthCode.isEmpty())
					authConn.setRequestProperty("Authorization", "basic " + JrSession.UserAuthCode);
				
				SAXParserFactory parserFactory = SAXParserFactory.newInstance();
				SAXParser sp = parserFactory.newSAXParser();
		    	JrResponseHandler jrResponseHandler = new JrResponseHandler();
		    	sp.parse(authConn.getInputStream(), jrResponseHandler);
		    	if (jrResponseHandler.getResponse().get(0).getItems().containsKey("Token"))
		    		token = jrResponseHandler.getResponse().get(0).getItems().get("Token");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return token;
		}
	}
}
