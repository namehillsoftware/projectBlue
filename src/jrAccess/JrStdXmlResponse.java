package jrAccess;

import java.io.IOException;
import java.net.MalformedURLException;
import android.os.AsyncTask;

public class JrStdXmlResponse extends AsyncTask<String, Void, JrResponse> {

	@Override
	protected JrResponse doInBackground(String... params) {
		JrResponse responseDao = null;
		
		JrConnection conn;
		try {
			conn = new JrConnection(params);
			conn.setConnectTimeout(5000);
	    	
	    	responseDao = JrResponse.fromInputStream(conn.getInputStream());
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
