package com.lasthopesoftware.bluewater.servers.library.items.media.files.access;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.File;

public class FileXmlResponse {

	public static List<File> GetFiles(InputStream is) {
		List<File> returnFiles = new ArrayList<File>();
		
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser sp = parserFactory.newSAXParser();
	    	FileXmlHandler jrFileXml = new FileXmlHandler();
	    	sp.parse(is, jrFileXml);
	    	
	    	returnFiles = jrFileXml.getFiles();
		} catch (MalformedURLException e) {
			LoggerFactory.getLogger(FileXmlResponse.class).error(e.toString(), e);
		} catch (IOException e) {
			LoggerFactory.getLogger(FileXmlResponse.class).error(e.toString(), e);
		} catch (SAXException e) {
			LoggerFactory.getLogger(FileXmlResponse.class).error(e.toString(), e);
		} catch (ParserConfigurationException e) {
			LoggerFactory.getLogger(FileXmlResponse.class).error(e.toString(), e);
		}
		
		return returnFiles;
	}
}
