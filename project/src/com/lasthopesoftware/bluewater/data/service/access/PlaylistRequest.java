package com.lasthopesoftware.bluewater.data.service.access;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;

public class PlaylistRequest {

	public static ArrayList<Playlist> GetItems(InputStream is) {
		ArrayList<Playlist> returnFiles = new ArrayList<Playlist>();
		
		try {			
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser sp = parserFactory.newSAXParser();
	    	PlaylistXmlHandler jrPlaylistXml = new PlaylistXmlHandler();
	    	sp.parse(is, jrPlaylistXml);
	    	
	    	returnFiles = jrPlaylistXml.getPlaylists();
		} catch (MalformedURLException e) {
			LoggerFactory.getLogger(PlaylistRequest.class).error(e.toString(), e);
		} catch (IOException e) {
			LoggerFactory.getLogger(PlaylistRequest.class).error(e.toString(), e);
		} catch (SAXException e) {
			LoggerFactory.getLogger(PlaylistRequest.class).error(e.toString(), e);
		} catch (ParserConfigurationException e) {
			LoggerFactory.getLogger(PlaylistRequest.class).error(e.toString(), e);
		}
		
		return returnFiles;
	}

}
