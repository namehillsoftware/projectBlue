package com.lasthopesoftware.bluewater.data.service.access;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import org.slf4j.LoggerFactory;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;

public class JrStringResponse extends AsyncTask<String, Void, String> {

	@Override
	protected String doInBackground(String... params) {
		try {
			HttpURLConnection conn = ConnectionManager.getConnection(params);
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
				String line;
				StringBuilder result = new StringBuilder();
				
				while ((line = br.readLine()) != null) {
					result.append(line);
				}
			
				return result.toString();
			} finally {
				conn.disconnect();
			}
		} catch (MalformedURLException e) {
			LoggerFactory.getLogger(JrStringResponse.class).error(e.toString(), e);
		} catch (IOException e) {
			LoggerFactory.getLogger(JrStringResponse.class).error(e.toString(), e);
		}
		
		return "";
	}

}
