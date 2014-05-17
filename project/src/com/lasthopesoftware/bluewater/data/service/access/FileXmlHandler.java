package com.lasthopesoftware.bluewater.data.service.access;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.FileUtils;

public class FileXmlHandler extends DefaultHandler {
	
	private ArrayList<File> files = new ArrayList<File>();
	private File currentFile;
	private String currentValue;
	private StringBuilder sb = null;
	private String currentKey;
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		
		currentValue = "";

		if (qName.equalsIgnoreCase("item"))
			currentFile = new File();
		
		if (qName.equalsIgnoreCase("field"))
			currentKey = attributes.getValue("Name");
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		sb = FileUtils.HandleBadXml(sb, ch, start, length);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (sb != null) currentValue = sb.toString();
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
