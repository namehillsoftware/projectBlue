package com.lasthopesoftware.bluewater.data.service.access;

import java.util.TreeMap;




import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.lasthopesoftware.bluewater.shared.XmlParsingHelpers;

public class FilePropertiesHandler extends DefaultHandler {
	
	private TreeMap<String, String> properties = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	private StringBuilder currentSb = null;
	private String currentKey;
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		currentSb = new StringBuilder();
		if (qName.equalsIgnoreCase("field"))
			currentKey = attributes.getValue("Name");
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		XmlParsingHelpers.HandleBadXml(currentSb, ch, start, length);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
//		if (sb != null) currentValue = sb.toString();
		
		if (qName.equalsIgnoreCase("field")) {
			properties.put(currentKey, currentSb.toString());
		}
			
	}
	
	
	/**
	 * @return the response
	 */
	public TreeMap<String, String> getProperties() {
		return properties;
	}
}
