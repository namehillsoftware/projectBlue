package com.lasthopesoftware.bluewater.data.service.access;

import java.io.IOException;
import java.net.MalformedURLException;

import org.slf4j.LoggerFactory;

import com.lasthopesoftware.bluewater.data.service.access.connection.JrConnection;

import android.os.AsyncTask;

public class JrStdXmlResponse extends AsyncTask<String, Void, JrResponse> {

	@Override
	protected JrResponse doInBackground(String... params) {
		JrResponse responseDao = null;
		
		try {
			JrConnection conn = new JrConnection(params);
	    	
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
