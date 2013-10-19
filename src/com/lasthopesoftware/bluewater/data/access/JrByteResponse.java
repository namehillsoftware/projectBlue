package com.lasthopesoftware.bluewater.data.access;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import com.lasthopesoftware.bluewater.data.access.connection.JrConnection;

import android.os.AsyncTask;

public class JrByteResponse extends AsyncTask<String, Void, byte[]> {

	@Override
	protected byte[] doInBackground(String... params) {
		InputStream is = null;
		// Add base url
	
		JrConnection conn;
		try {
			conn = new JrConnection(params);
			is = conn.getInputStream();
			int nRead = 0;
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] data = new byte[16384];
			
			while ((nRead = is.read(data, 0, data.length)) != -1)
				buffer.write(data, 0, nRead);
			
			return buffer.toByteArray();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new byte[0];
	}

}
