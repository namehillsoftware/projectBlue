package jrAccess;

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class JrStdResponseHandler extends DefaultHandler {
	
	private List<JrResponse> response = new ArrayList<JrResponse>();
	private JrResponse currentResponse;
	private String currentValue;
	private String currentKey;
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		currentValue = "";
		currentKey = "";
		if (qName.equalsIgnoreCase("response"))
			currentResponse = new JrResponse(attributes.getValue("Status"));
		
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
			currentResponse.items.put(currentKey, currentValue);
	}
	
	/**
	 * @return the response
	 */
	public List<JrResponse> getResponse() {
		return response;
	}

	/**
	 * @param response the response to set
	 */
	public void setResponse(List<JrResponse> response) {
		this.response = response;
	}
}
