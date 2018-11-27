package com.lasthopesoftware.bluewater.client.library.items.access;

import com.lasthopesoftware.bluewater.client.library.items.Item;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

class ItemResponseHandler extends DefaultHandler {

	private String currentValue;
	private String currentKey;
	
	public final List<Item> items = new ArrayList<>();

	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		currentValue = "";
		currentKey = "";
		
		if (qName.equalsIgnoreCase("item"))
			currentKey = attributes.getValue("Name");
	}
	
	public void characters(char[] ch, int start, int length) {
		currentValue = new String(ch,start,length);
	}
	
	public void endElement(String uri, String localName, String qName) {
		if (qName.equalsIgnoreCase("item"))
			items.add(new Item(Integer.parseInt(currentValue), currentKey));
	}
}
