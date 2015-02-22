package com.lasthopesoftware.bluewater.servers.connection;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class LookUpResponseHandler extends DefaultHandler {
		
	private AccessConfiguration response;
	private String currentValue;
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		currentValue = "";
		if (qName.equalsIgnoreCase("response")) {
			response = new AccessConfiguration();
			response.setStatus(attributes.getValue("Status").equalsIgnoreCase("OK"));
		}
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		currentValue = new String(ch,start,length);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("ip"))
			response.setRemoteIp(currentValue);
		
		if (qName.equalsIgnoreCase("port"))
			response.setPort(Integer.parseInt(currentValue));
		
		if (qName.equalsIgnoreCase("localiplist")) {
			for (String ip : currentValue.split(","))
				response.getLocalIps().add(ip.trim());
		}
		
		if (qName.equalsIgnoreCase("macaddresslist")) {
			for (String mac : currentValue.split(","))
				response.getMacAddresses().add(mac.trim());
		}
	}
	
	/**
	 * @return the response
	 */
	public AccessConfiguration getResponse() {
		return response;
	}
}
