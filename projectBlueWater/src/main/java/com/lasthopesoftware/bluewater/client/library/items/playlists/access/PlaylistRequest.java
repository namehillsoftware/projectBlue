package com.lasthopesoftware.bluewater.client.library.items.playlists.access;

import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

class PlaylistRequest {

	public static ArrayList<Playlist> GetItems(InputStream is) {
	
		try {			
			final SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
	    	PlaylistXmlHandler jrPlaylistXml = new PlaylistXmlHandler();
	    	sp.parse(is, jrPlaylistXml);
	    	
	    	return jrPlaylistXml.getPlaylists();
		} catch (IOException | SAXException | ParserConfigurationException e) {
			LoggerFactory.getLogger(PlaylistRequest.class).error(e.toString(), e);
		}
		
		return new ArrayList<>();
	}

}
