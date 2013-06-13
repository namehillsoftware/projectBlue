package jrAccess;

import java.util.TreeMap;

import jrFileSystem.JrFileUtils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class JrFilePropertiesHandler extends DefaultHandler {
	
	private TreeMap<String, String> properties = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	private String currentValue;
	private StringBuilder sb = null;
	private String currentKey;
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		currentValue = "";
		if (qName.equalsIgnoreCase("field"))
			currentKey = attributes.getValue("Name");
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		sb = JrFileUtils.HandleBadXml(sb, ch, start, length);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (sb != null) currentValue = sb.toString();
		
		if (qName.equalsIgnoreCase("field")) {
			properties.put(currentKey, currentValue);
		}
			
	}
	
	
	/**
	 * @return the response
	 */
	public TreeMap<String, String> getProperties() {
		return properties;
	}
}
