package com.lasthopesoftware.bluewater.data.service.access;

import java.util.ArrayList;
import java.util.TreeMap;





import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.shared.XmlParsingHelpers;

public class PlaylistXmlHandler extends DefaultHandler {
	
	private TreeMap<String, Playlist> playlists = new TreeMap<String, Playlist>();
	private Playlist currentPlaylist;
	private String currentValue;
	private String currentKey;
	private StringBuilder valueSb;
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		currentValue = "";
		valueSb = new StringBuilder();
		if (qName.equalsIgnoreCase("item"))
			currentPlaylist = new Playlist();
		
		if (qName.equalsIgnoreCase("field"))
			currentKey = attributes.getValue("Name");
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		XmlParsingHelpers.HandleBadXml(valueSb, ch, start, length);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (valueSb != null) currentValue = valueSb.toString();
		if (qName.equalsIgnoreCase("field")) {
			if (currentKey.equalsIgnoreCase("id"))
				currentPlaylist.setKey(Integer.parseInt(currentValue));
			
			if (currentKey.equalsIgnoreCase("group"))
				currentPlaylist.setGroup(currentValue);
			
			if (currentKey.equalsIgnoreCase("name"))
				currentPlaylist.setValue(currentValue);
			
			if (currentKey.equalsIgnoreCase("path")) {
				currentPlaylist.setPath(currentValue);
				playlists.put(currentValue, currentPlaylist);
				
				// Add existing children
				for (String key : playlists.keySet()) {
					int lastKeyPathIndex = key.lastIndexOf('\\');
					if (lastKeyPathIndex > -1 && key.indexOf(currentValue) == 0 && key.equals(currentValue + "\\" + key.substring(lastKeyPathIndex + 1))) {
						currentPlaylist.addPlaylist(playlists.get(key));
					}
				}
				
				// Add to existing parent if it has a path
				int lastPathIndex = currentValue.lastIndexOf('\\');
				if (lastPathIndex > -1) {
					String parent = currentValue.substring(0, lastPathIndex);
					if (playlists.containsKey(parent)) {
						playlists.get(parent).addPlaylist(currentPlaylist);
					}
				}
			}
		}
			
	}
	
	/**
	 * @return the response
	 */
	public ArrayList<Playlist> getPlaylists() {
		ArrayList<Playlist> returnList = new ArrayList<Playlist>(playlists.values());
		
		return returnList;
	}
}
