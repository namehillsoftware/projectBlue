package com.lasthopesoftware.bluewater.data.service.access;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.service.objects.JrFileUtils;

public class JrFileXmlHandler extends DefaultHandler {
	
	private ArrayList<JrFile> files = new ArrayList<JrFile>();
	private JrFile currentFile;
	private String currentValue;
	private StringBuilder sb = null;
	private String currentKey;
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		
		currentValue = "";

		if (qName.equalsIgnoreCase("item"))
			currentFile = new JrFile();
		
		if (qName.equalsIgnoreCase("field"))
			currentKey = attributes.getValue("Name");
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		sb = JrFileUtils.HandleBadXml(sb, ch, start, length);
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
	public ArrayList<JrFile> getFiles() {
		return files;
	}
}
