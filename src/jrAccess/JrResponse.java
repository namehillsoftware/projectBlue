package jrAccess;

import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class JrResponse {

	private boolean status;
	public HashMap<String, String> items = new HashMap<String, String>();
	
	public JrResponse(String status) {
		this.status = status != null && status.equalsIgnoreCase("OK");
	}
	
	public static JrResponse fromInputStream(InputStream is) {
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser sp = parserFactory.newSAXParser();
	    	JrStdResponseHandler jrResponseHandler = new JrStdResponseHandler();
			sp.parse(is, jrResponseHandler);
			return jrResponseHandler.getResponse().get(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return null;
	}

	/**
	 * @return the status
	 */
	public boolean isStatus() {
		return status;
	}	
}
