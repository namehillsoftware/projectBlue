package com.lasthopesoftware.jrmediastreamer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

import android.os.AsyncTask;
import android.util.Base64;

public class GetJrResponse extends AsyncTask<String, Void, JrResponseDao> {

	@Override
	protected JrResponseDao doInBackground(String... params) {
		JrResponseDao responseDao = null;
		// Add base url
		String url = params[0];
		
		// Get authentication token
		if (JrAuth.token.isEmpty()) {
			try {
				URLConnection authConn = (new URL(url + "Authenticate")).openConnection();
				
				if (!JrAuth.UserName.isEmpty() || !JrAuth.Password.isEmpty())
					authConn.setRequestProperty("Authorization", "basic " + Base64.encodeToString((JrAuth.UserName + ":" + JrAuth.Password).getBytes(), Base64.DEFAULT));
				
				System.out.println(authConn.getRequestProperty("Authorization"));
				
				SAXParserFactory parserFactory = SAXParserFactory.newInstance();
				SAXParser sp = parserFactory.newSAXParser();
		    	JrResponseHandler jrResponseHandler = new JrResponseHandler();
		    	sp.parse(authConn.getInputStream(), jrResponseHandler);
		    	JrAuth.token = jrResponseHandler.getResponse().get(0).getItems().get("Token");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Add action
		url += params[1];
		
		// Add token
		url += "?Token=" + JrAuth.token;
		
		// add arguments
		if (params.length > 2) {
			for (int i = 2; i < params.length; i++) {
				url += "&" + params[i];
			}
		}
		
		URLConnection conn;
		try {
			conn = (new URL(url)).openConnection();
			
			
			
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser sp = parserFactory.newSAXParser();
	    	JrResponseHandler jrResponseHandler = new JrResponseHandler();
	    	sp.parse(conn.getInputStream(), jrResponseHandler);
	    	
	    	responseDao = jrResponseHandler.getResponse().get(0);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseDao;
	}

}
