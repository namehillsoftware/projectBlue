package com.lasthopesoftware.bluewater.data.service.access;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.lasthopesoftware.bluewater.data.service.objects.JrItem;

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
			LoggerFactory.getLogger(JrFsResponse.class).error(e.toString(), e);
		} catch (IOException e) {
			LoggerFactory.getLogger(JrFsResponse.class).error(e.toString(), e);
		} catch (SAXException e) {
			LoggerFactory.getLogger(JrFsResponse.class).error(e.toString(), e);
		} catch (ParserConfigurationException e) {
			LoggerFactory.getLogger(JrFsResponse.class).error(e.toString(), e);
		}
		
		return items;
	}

}
