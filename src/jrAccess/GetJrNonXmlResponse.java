package jrAccess;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.os.AsyncTask;

public class GetJrNonXmlResponse extends AsyncTask<String, Void, InputStream> {

	@Override
	protected InputStream doInBackground(String... params) {
		InputStream is = null;
		// Add base url
		String url = params[0];
		
		// Add action
		url += params[1];
		
		// Add token
		if (params.length >= 3)
			url += "?Token=" + params[2];
		
		// add arguments
		if (params.length > 3) {
			for (int i = 3; i < params.length; i++) {
				url += "&" + params[i];
			}
		}
		
		URLConnection conn;
		try {
			conn = (new URL(url)).openConnection();
			conn.setConnectTimeout(5000);
			
			return conn.getInputStream();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return is;
	}

}
