package jrAccess;

import java.util.ArrayList;
import jrFileSystem.IJrItem;
import jrFileSystem.JrFile;
import jrFileSystem.JrListing;

public class JrSession {
	public static String UserAuthCode = "";
	public static String AccessCode = "";
	
	public static JrAccessDao accessDao;
    public static IJrItem<?> selectedLibrary;
    
    public static JrListing selectedItem;
    public static JrFile playingFile;
    public static ArrayList<JrFile> playlist;
    
}
