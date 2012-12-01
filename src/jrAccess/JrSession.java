package jrAccess;

import android.media.MediaPlayer;
import jrFileSystem.JrFile;
import jrFileSystem.JrFileSystem;
import jrFileSystem.JrListing;
import jrFileSystem.JrPage;

public class JrSession {
	public static String UserAuthCode = "";
	public static String AccessCode = "";
	
	public static JrAccessDao accessDao;
    public static JrPage selectedLibrary;
    
    public static JrListing selectedItem;
    public static JrFile playingFile;
    
    public static MediaPlayer mMediaPlayer;
}
