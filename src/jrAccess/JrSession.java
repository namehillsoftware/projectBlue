package jrAccess;

import java.util.ArrayList;
import jrFileSystem.JrFile;
import jrFileSystem.JrListing;
import jrFileSystem.JrPage;

public class JrSession {
	public static String UserAuthCode = "";
	public static String AccessCode = "";
	
	public static JrAccessDao accessDao;
    public static JrPage selectedLibrary;
    
    public static JrListing selectedItem;
    public static JrFile playingFile;
    public static ArrayList<JrFile> playlist;
    
}
