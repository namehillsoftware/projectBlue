package jrFileSystem;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;

import jrAccess.GetJrNonXmlResponse;
import jrAccess.JrSession;
import android.os.AsyncTask;

public class JrFile extends JrListing {

	public JrFile(int key) {
		this.key = key;
	}
	public JrFile(int key, String value) {
		super(key, value);
	}
	
	public JrFile() {
		super();
	}
	
	public byte[] getFile() {		
		byte[] returnFile = null;
		
		try {
			(new GetJrNonXmlResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "File/GetFile", JrSession.accessDao.getToken(), "File=" + String.valueOf(key), "PlayBack=0", "FileType=Key" }).get().read(returnFile);
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return returnFile;
	}
	
	
}
