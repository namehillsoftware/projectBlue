package com.lasthopesoftware.bluewater.data.service.access;

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

import com.lasthopesoftware.bluewater.data.service.objects.JrFile;

public class JrFileXmlResponse {

	public static List<JrFile> GetFiles(InputStream is) {
		List<JrFile> returnFiles = new ArrayList<JrFile>();
		
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser sp = parserFactory.newSAXParser();
	    	JrFileXmlHandler jrFileXml = new JrFileXmlHandler();
	    	sp.parse(is, jrFileXml);
	    	
	    	returnFiles = jrFileXml.getFiles();
		} catch (MalformedURLException e) {
			LoggerFactory.getLogger(JrFileXmlResponse.class).error(e.toString(), e);
		} catch (IOException e) {
			LoggerFactory.getLogger(JrFileXmlResponse.class).error(e.toString(), e);
		} catch (SAXException e) {
			LoggerFactory.getLogger(JrFileXmlResponse.class).error(e.toString(), e);
		} catch (ParserConfigurationException e) {
			LoggerFactory.getLogger(JrFileXmlResponse.class).error(e.toString(), e);
		}
		
		return returnFiles;
	}
}
