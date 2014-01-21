package com.lasthopesoftware.bluewater.data.service.access;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class JrStdResponseHandler extends DefaultHandler {
	
	private JrResponse response;
	private String currentValue;
	private String currentKey;
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		currentValue = "";
		currentKey = "";
		if (qName.equalsIgnoreCase("response"))
			response = new JrResponse(attributes.getValue("Status"));
		
		if (qName.equalsIgnoreCase("item")) {
			currentKey = attributes.getValue("Name");
			
		}
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		currentValue = new String(ch,start,length);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("item"))
			response.items.put(currentKey, currentValue);
	}
	
	/**
	 * @return the response
	 */
	public JrResponse getResponse() {
		return response;
	}
}
