package jrAccess;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jrFileSystem.JrPlaylist;

import org.xml.sax.SAXException;

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return returnFiles;
	}

}
