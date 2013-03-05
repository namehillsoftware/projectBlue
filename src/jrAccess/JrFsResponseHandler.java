package jrAccess;

import java.util.ArrayList;
import java.util.List;
import jrFileSystem.JrFileUtils;
import jrFileSystem.JrListing;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class JrFsResponseHandler<T extends JrListing> extends DefaultHandler {
	
	private JrResponseDao currentResponse;
	
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
//		if (qName.equalsIgnoreCase("response"))
//			response.add(currentResponse);
		
		if (qName.equalsIgnoreCase("item")) {
			T newItem = (T) JrFileUtils.createListing(newClass);
			newItem.mKey = Integer.parseInt(currentValue);
			newItem.mValue = currentKey;
			items.add(newItem);
		}
	}
}
