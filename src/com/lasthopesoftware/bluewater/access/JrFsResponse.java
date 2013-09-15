package com.lasthopesoftware.bluewater.access;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


import org.xml.sax.SAXException;

import com.lasthopesoftware.bluewater.FileSystem.JrItem;

public class JrFsResponse {

	public static List<JrItem> GetItems(InputStream is) {
		List<JrItem> items = new ArrayList<JrItem>();

		try {
			
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser sp = parserFactory.newSAXParser();
	    	JrFsResponseHandler<JrItem> jrResponseHandler = new JrFsResponseHandler<JrItem>(JrItem.class);
	    	sp.parse(is, jrResponseHandler);
	    	
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
