package jrAccess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import android.os.AsyncTask;

public class JrStringResponse extends AsyncTask<String, Void, String> {

	@Override
	protected String doInBackground(String... params) {
				
		JrConnection conn;
		try {
			conn = new JrConnection(params);
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			StringBuilder result = new StringBuilder();
			
			while ((line = br.readLine()) != null) {
				result.append(line);
			}
			
			return result.toString();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "";
	}

}
