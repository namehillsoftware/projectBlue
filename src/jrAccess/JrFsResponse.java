package jrAccess;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import jrFileSystem.JrItem;
import jrFileSystem.JrListing;
import org.xml.sax.SAXException;
import android.os.AsyncTask;

public class JrFsResponse<T extends JrListing> extends AsyncTask<String, Void, List<JrItem>> {

	private Class<JrItem> newClass;
	
	public JrFsResponse(Class<JrItem> c) {
		newClass = c;
	}
	
	@Override
	protected List<JrItem> doInBackground(String... params) {
		List<JrItem> items = new ArrayList<JrItem>();
		
		// Add base url
		String url = JrSession.accessDao.getJrUrl(params);
		
		URLConnection conn;
		try {
			conn = (new URL(url)).openConnection();
			conn.setConnectTimeout(5000);
			
			
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser sp = parserFactory.newSAXParser();
	    	JrFsResponseHandler<JrItem> jrResponseHandler = new JrFsResponseHandler<JrItem>(newClass);
	    	sp.parse(conn.getInputStream(), jrResponseHandler);
	    	
	    	items = jrResponseHandler.items;
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
		
		return items;
	}

}
