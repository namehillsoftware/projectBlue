package com.lasthopesoftware.bluewater.data.access;

import java.io.IOException;
import java.net.MalformedURLException;

import com.lasthopesoftware.bluewater.data.access.connection.JrConnection;

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseDao;
	}

}
