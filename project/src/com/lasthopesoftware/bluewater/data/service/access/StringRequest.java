package com.lasthopesoftware.bluewater.data.service.access;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import org.slf4j.LoggerFactory;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionManager;

public class StringRequest extends AsyncTask<String, Void, String> {

	@Override
	protected String doInBackground(String... params) {
		try {
			final HttpURLConnection conn = ConnectionManager.getConnection(params);
			try {
				final InputStream is = conn.getInputStream();
				try {
					final InputStreamReader isr = new InputStreamReader(is);
					try {
						final BufferedReader br = new BufferedReader(isr);
						
						try {
							String line;
							final StringBuilder result = new StringBuilder();
							
							while ((line = br.readLine()) != null) {
								result.append(line);
							}
						
							return result.toString();
						} finally {
							br.close();
						}
					} finally {
						isr.close();
					}
				} finally {
					is.close();
				}
			} finally {
				conn.disconnect();
			}
		} catch (MalformedURLException e) {
			LoggerFactory.getLogger(StringRequest.class).error(e.toString(), e);
		} catch (IOException e) {
			LoggerFactory.getLogger(StringRequest.class).error(e.toString(), e);
		}
		
		return "";
	}

}
