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

	String mArtist;
	String mAlbum;
	String mGenre;
	int mTrackNumber;
	
	public JrFile(int key) {
		this.mKey = key;
	}
	public JrFile(int key, String value) {
		super(key, value);
	}
	
	public JrFile(int key, String value, String Artist, String Album, int TrackNumber) {
		super(key, value);
		
		mArtist = Artist;
		mAlbum = Album;
		mTrackNumber = TrackNumber;
	}
	
	public JrFile() {
		super();
	}
	
	public byte[] getFile() {		
		byte[] returnFile = null;
		
		try {
			(new GetJrNonXmlResponse()).execute(new String[] { "File/GetFile", "File=" + String.valueOf(mKey), "PlayBack=0", "FileType=Key" }).get().read(returnFile);
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return returnFile;
	}
	
}
