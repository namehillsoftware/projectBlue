package com.lasthopesoftware.bluewater.data.service.access;

import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.LoggerFactory;

public class StandardRequest {

	private final boolean status;
	public HashMap<String, String> items = new HashMap<String, String>();
	
	public StandardRequest(String status) {
		this.status = status != null && status.equalsIgnoreCase("OK");
	}

	/**
	 * @return the status
	 */
	public boolean isStatus() {
		return status;
	}
	
	public static StandardRequest fromInputStream(InputStream is) {
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser sp = parserFactory.newSAXParser();
	    	StandardResponseHandler jrResponseHandler = new StandardResponseHandler();
			sp.parse(is, jrResponseHandler);
			return jrResponseHandler.getResponse();
		} catch (Exception e) {
			LoggerFactory.getLogger(StandardRequest.class).error(e.toString(), e);
		}
    	
    	return null;
	}
}
