package com.lasthopesoftware.bluewater.data.service.access;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import org.slf4j.LoggerFactory;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;

public class JrStdXmlResponse extends AsyncTask<String, Void, JrResponse> {

	@Override
	protected JrResponse doInBackground(String... params) {
		JrResponse responseDao = null;
		
		try {
			HttpURLConnection conn = ConnectionManager.getConnection(params);
	    	
			try {
				responseDao = JrResponse.fromInputStream(conn.getInputStream());
			} finally {
				conn.disconnect();
			}
		} catch (MalformedURLException e) {
			LoggerFactory.getLogger(JrStdXmlResponse.class).error(e.toString(), e);
		} catch (IOException e) {
			LoggerFactory.getLogger(JrStdXmlResponse.class).error(e.toString(), e);
		}
		
		return responseDao;
	}

}
