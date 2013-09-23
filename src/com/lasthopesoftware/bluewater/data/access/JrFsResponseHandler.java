package com.lasthopesoftware.bluewater.data.access;

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.lasthopesoftware.bluewater.data.objects.JrFileUtils;
import com.lasthopesoftware.bluewater.data.objects.JrObject;

public class JrFsResponseHandler<T extends JrObject> extends DefaultHandler {
	
	private String currentValue;
	private String currentKey;
	
	public List<T> items = new ArrayList<T>();
	
	private Class<T> newClass;
	
	public JrFsResponseHandler(Class<T> c) {
		newClass = c;
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		currentValue = "";
		currentKey = "";
		
		if (qName.equalsIgnoreCase("item")) {
			currentKey = attributes.getValue("Name");
			
		}
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		currentValue = new String(ch,start,length);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
//		if (qName.equalsIgnoreCase("response"))
//			response.add(currentResponse);
		
		if (qName.equalsIgnoreCase("item")) {
			T newItem = (T) JrFileUtils.createListing(newClass);
			newItem.setKey(Integer.parseInt(currentValue));
			newItem.setValue(currentKey);
			items.add(newItem);
		}
	}
}
