package jrFileSystem;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;

import jrAccess.JrStringResponse;
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
			//(new GetJrStringResponse()).execute(new String[] { "File/GetFile", "File=" + String.valueOf(mKey), "PlayBack=0", "FileType=Key" }).get().read(returnFile);
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return returnFile;
	}
	/**
	 * @return the mArtist
	 */
	public String getArtist() {
		return mArtist;
	}
	/**
	 * @param mArtist the mArtist to set
	 */
	public void setArtist(String mArtist) {
		this.mArtist = mArtist;
	}
	/**
	 * @return the mAlbum
	 */
	public String getAlbum() {
		return mAlbum;
	}
	/**
	 * @param mAlbum the mAlbum to set
	 */
	public void setAlbum(String mAlbum) {
		this.mAlbum = mAlbum;
	}
	/**
	 * @return the mGenre
	 */
	public String getGenre() {
		return mGenre;
	}
	/**
	 * @param mGenre the mGenre to set
	 */
	public void setGenre(String mGenre) {
		this.mGenre = mGenre;
	}
	/**
	 * @return the mTrackNumber
	 */
	public int getTrackNumber() {
		return mTrackNumber;
	}
	/**
	 * @param mTrackNumber the mTrackNumber to set
	 */
	public void setTrackNumber(int mTrackNumber) {
		this.mTrackNumber = mTrackNumber;
	}
	
}
