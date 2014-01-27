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

import com.lasthopesoftware.bluewater.data.service.objects.JrPlaylist;

public class JrPlaylistResponse {

	public static ArrayList<JrPlaylist> GetItems(InputStream is) {
		ArrayList<JrPlaylist> returnFiles = new ArrayList<JrPlaylist>();
		
		try {			
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser sp = parserFactory.newSAXParser();
	    	JrPlaylistXmlHandler jrPlaylistXml = new JrPlaylistXmlHandler();
	    	sp.parse(is, jrPlaylistXml);
	    	
	    	returnFiles = jrPlaylistXml.getPlaylists();
		} catch (MalformedURLException e) {
			LoggerFactory.getLogger(JrPlaylistResponse.class).error(e.toString(), e);
		} catch (IOException e) {
			LoggerFactory.getLogger(JrPlaylistResponse.class).error(e.toString(), e);
		} catch (SAXException e) {
			LoggerFactory.getLogger(JrPlaylistResponse.class).error(e.toString(), e);
		} catch (ParserConfigurationException e) {
			LoggerFactory.getLogger(JrPlaylistResponse.class).error(e.toString(), e);
		}
		
		return returnFiles;
	}

}
