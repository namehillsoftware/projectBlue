package jrAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jrFileSystem.JrFile;
import jrFileSystem.JrFileUtils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import xmlwise.XmlElement;
import xmlwise.XmlParseException;
import xmlwise.Xmlwise;

public class JrFileXmlHandler extends DefaultHandler {
	
	private ArrayList<JrFile> files = new ArrayList<JrFile>();
	private JrFile currentFile;
	private String currentValue;
	private StringBuilder sb = null;
	private String currentKey;
	private String newValue;
	
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
			else if (currentKey.equalsIgnoreCase("Artist"))
				currentFile.setArtist(currentValue);
			else if (currentKey.equalsIgnoreCase("Album"))
				currentFile.setAlbum(currentValue);
			else if (currentKey.equalsIgnoreCase("Name"))
				currentFile.setValue(currentValue);
			else if (currentKey.equalsIgnoreCase("Genre"))
				currentFile.setGenre(currentValue);
			else if (currentKey.equalsIgnoreCase("Track #"))
				currentFile.setTrackNumber(Integer.parseInt(currentValue));
			else if (currentKey.equalsIgnoreCase("Duration"))
				currentFile.setDuration(Double.parseDouble(currentValue) * 1000);
		}
			
	}
	
	
	/**
	 * @return the response
	 */
	public ArrayList<JrFile> getFiles() {
		return files;
	}
}
