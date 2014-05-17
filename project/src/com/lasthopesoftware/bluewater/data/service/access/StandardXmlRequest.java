package com.lasthopesoftware.bluewater.data.service.access;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import org.slf4j.LoggerFactory;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;

public class StandardXmlRequest extends AsyncTask<String, Void, StandardRequest> {

	@Override
	protected StandardRequest doInBackground(String... params) {
		StandardRequest responseDao = null;
		
		try {
			HttpURLConnection conn = ConnectionManager.getConnection(params);
	    	
			try {
				responseDao = StandardRequest.fromInputStream(conn.getInputStream());
			} finally {
				conn.disconnect();
			}
		} catch (MalformedURLException e) {
			LoggerFactory.getLogger(StandardXmlRequest.class).error(e.toString(), e);
		} catch (IOException e) {
			LoggerFactory.getLogger(StandardXmlRequest.class).error(e.toString(), e);
		}
		
		return responseDao;
	}

}
