package jrAccess;

import java.util.ArrayList;
import jrFileSystem.IJrItem;
import jrFileSystem.JrFile;
import jrFileSystem.JrFileSystem;

public class JrSession {
	public static String UserAuthCode = "";
	public static String AccessCode = "";
	
	public static JrAccessDao accessDao;
    public static ArrayList<IJrItem> categories;
    
    public static IJrItem<?> selectedItem;
    public static JrFile playingFile;
    public static ArrayList<JrFile> playlist;
    
    public static JrFileSystem jrFs;
    
}
