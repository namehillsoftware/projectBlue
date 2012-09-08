package com.lasthopesoftware.jrmediastreamer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class JrResponseHandler extends DefaultHandler {
	
	private List<JrResponseDao> response = new ArrayList<JrResponseDao>();
	private JrResponseDao currentResponse;
	private String currentValue;
	private String currentKey;
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		currentValue = "";
		currentKey = "";
		if (qName.equalsIgnoreCase("response"))
			currentResponse = new JrResponseDao(attributes.getValue("Status"));
		
		if (qName.equalsIgnoreCase("item")) {
			currentKey = attributes.getValue("Name");
			
		}
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		currentValue = new String(ch,start,length);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("response"))
			response.add(currentResponse);
		
		if (qName.equalsIgnoreCase("item"))
			currentResponse.getItems().put(currentKey, currentValue);
	}
	
	/**
	 * @return the response
	 */
	public List<JrResponseDao> getResponse() {
		return response;
	}

	/**
	 * @param response the response to set
	 */
	public void setResponse(List<JrResponseDao> response) {
		this.response = response;
	}
}
