package jrAccess;

import java.util.ArrayList;
import java.util.List;
import jrFileSystem.JrFile;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class JrFileXmlHandler extends DefaultHandler {
	
	private List<JrFile> files = new ArrayList<JrFile>();
	private JrFile currentFile;
	private String currentValue;
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
		currentValue = new String(ch,start,length);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("item"))
			files.add(currentFile);
		
		if (qName.equalsIgnoreCase("field")) {
			if (currentKey.equalsIgnoreCase("Key"))
				currentFile.setKey(Integer.parseInt(currentValue));
			
			if (currentKey.equalsIgnoreCase("Artist"))
				currentFile.setArtist(currentValue);
			
			if (currentKey.equalsIgnoreCase("Album"))
				currentFile.setAlbum(currentValue);
			
			if (currentKey.equalsIgnoreCase("Name"))
				currentFile.setValue(currentValue);
			
			if (currentKey.equalsIgnoreCase("Genre"))
				currentFile.setGenre(currentValue);
			
			if (currentKey.equalsIgnoreCase("Track #"))
				currentFile.setTrackNumber(Integer.parseInt(currentValue));
			
			if (currentKey.equalsIgnoreCase("Duration"))
				currentFile.setDuration(Double.parseDouble(currentValue) * 1000);
		}
			
	}
	
	/**
	 * @return the response
	 */
	public List<JrFile> getFiles() {
		return files;
	}
}
