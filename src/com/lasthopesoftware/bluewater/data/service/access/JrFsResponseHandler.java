package com.lasthopesoftware.bluewater.data.service.access;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.lasthopesoftware.bluewater.data.service.objects.JrObject;

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
		
		if (qName.equalsIgnoreCase("item")) {
			T newItem;
			try {
				newItem = newClass.newInstance();
				newItem.setKey(Integer.parseInt(currentValue));
				newItem.setValue(currentKey);
				items.add(newItem);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
