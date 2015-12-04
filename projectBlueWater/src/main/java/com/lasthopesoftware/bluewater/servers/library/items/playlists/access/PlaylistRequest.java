package com.lasthopesoftware.bluewater.servers.library.items.playlists.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;

import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

class PlaylistRequest {

	public static ArrayList<Playlist> GetItems(ConnectionProvider connectionProvider, InputStream is) {
	
		try {			
			final SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
	    	PlaylistXmlHandler jrPlaylistXml = new PlaylistXmlHandler(connectionProvider);
	    	sp.parse(is, jrPlaylistXml);
	    	
	    	return jrPlaylistXml.getPlaylists();
		} catch (IOException | SAXException | ParserConfigurationException e) {
			LoggerFactory.getLogger(PlaylistRequest.class).error(e.toString(), e);
		}
		
		return new ArrayList<>();
	}

}
