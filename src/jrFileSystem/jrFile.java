package jrFileSystem;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import jrAccess.JrResponseHandler;
import jrAccess.JrSession;
import android.os.AsyncTask;
import android.util.Base64;

public class jrFile extends jrListing {

	public jrFile(int key, String value) {
		super(key, value);
	}
	
	public jrFile() {
		super();
	}
	
	public byte[] getFile() {		
		byte[] returnFile = null;
		
		try {
			returnFile = (new GetJrFile()).execute(new String[] { JrSession.accessDao.getValidUrl(), String.valueOf(key) }).get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return returnFile;
//		http://localhost:52199/MCWS/v1/File/GetFile?File=1&FileType=key
	}
	
	public class GetJrFile extends AsyncTask<String, Void, byte[]> {

		@Override
		protected byte[] doInBackground(String... params) {
			byte[] returnFile = null;
			// Add base url
			String url = params[0];
			
			// Get authentication token
			if (JrSession.token.isEmpty()) {
				try {
					URLConnection authConn = (new URL(url + "Authenticate")).openConnection();
					
					if (!JrSession.UserName.isEmpty() || !JrSession.Password.isEmpty())
						authConn.setRequestProperty("Authorization", "basic " + Base64.encodeToString((JrSession.UserName + ":" + JrSession.Password).getBytes(), Base64.DEFAULT));
					
					System.out.println(authConn.getRequestProperty("Authorization"));
					
					SAXParserFactory parserFactory = SAXParserFactory.newInstance();
					SAXParser sp = parserFactory.newSAXParser();
			    	JrResponseHandler jrResponseHandler = new JrResponseHandler();
			    	sp.parse(authConn.getInputStream(), jrResponseHandler);
			    	JrSession.token = jrResponseHandler.getResponse().get(0).getItems().get("Token");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// Add action
			url += "File/GetFile";
			
			// Add token
			url += "?Token=" + JrSession.token;
			
			// add arguments
			url += "&File=" + params[0] + "&PlayBack=0&FileType=Key"; 
			
			URLConnection conn;
			try {
				conn = (new URL(url)).openConnection();
				
				conn.getInputStream().read(returnFile);
		    	
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return returnFile;
		}

	}
}
