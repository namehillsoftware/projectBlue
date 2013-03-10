package jrAccess;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import android.os.AsyncTask;

public class JrStdXmlResponse extends AsyncTask<String, Void, JrResponseDao> {

	@Override
	protected JrResponseDao doInBackground(String... params) {
		JrResponseDao responseDao = null;
		
		// Add base url
		String url = JrSession.accessDao.getJrUrl(params);
		
		URLConnection conn;
		try {
			conn = (new URL(url)).openConnection();
			conn.setConnectTimeout(5000);
			
			
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser sp = parserFactory.newSAXParser();
	    	JrStdResponseHandler jrResponseHandler = new JrStdResponseHandler();
	    	sp.parse(conn.getInputStream(), jrResponseHandler);
	    	
	    	responseDao = jrResponseHandler.getResponse().get(0);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseDao;
	}

}
