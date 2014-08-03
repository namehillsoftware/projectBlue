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

import com.lasthopesoftware.bluewater.data.service.objects.Item;

public class FilesystemResponse {

	public static List<Item> GetItems(InputStream is) {
		List<Item> items = new ArrayList<Item>();

		try {
			
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser sp = parserFactory.newSAXParser();
	    	FilesystemResponseHandler<Item> jrResponseHandler = new FilesystemResponseHandler<Item>(Item.class);
	    	sp.parse(is, jrResponseHandler);
	    	
	    	items = jrResponseHandler.items;
		} catch (MalformedURLException e) {
			LoggerFactory.getLogger(FilesystemResponse.class).error(e.toString(), e);
		} catch (IOException e) {
			LoggerFactory.getLogger(FilesystemResponse.class).error(e.toString(), e);
		} catch (SAXException e) {
			LoggerFactory.getLogger(FilesystemResponse.class).error(e.toString(), e);
		} catch (ParserConfigurationException e) {
			LoggerFactory.getLogger(FilesystemResponse.class).error(e.toString(), e);
		}
		
		return items;
	}

}
