package com.lasthopesoftware.bluewater.servers.library.items.media.files.access;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.File;
import com.lasthopesoftware.bluewater.shared.XmlParsingHelpers;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

public class FileXmlHandler extends DefaultHandler {
	
	private ArrayList<File> files = new ArrayList<>();
	private File currentFile;
	private String currentValue;
	private StringBuilder currentSb = null;
	private String currentKey;
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		currentSb = new StringBuilder();
		currentValue = "";

		if (qName.equalsIgnoreCase("item"))
			currentFile = new File();
		
		if (qName.equalsIgnoreCase("field"))
			currentKey = attributes.getValue("Name");
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		XmlParsingHelpers.HandleBadXml(currentSb, ch, start, length);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (currentSb != null) currentValue = currentSb.toString();
		if (qName.equalsIgnoreCase("item"))
			files.add(currentFile);
		
		if (qName.equalsIgnoreCase("field")) {
			if (currentKey.equalsIgnoreCase("Key"))
				currentFile.setKey(Integer.parseInt(currentValue));
			else if (currentKey.equalsIgnoreCase("Name"))
				currentFile.setValue(currentValue);
		}
	}
	
	
	/**
	 * @return the response
	 */
	public ArrayList<File> getFiles() {
		return files;
	}
}
