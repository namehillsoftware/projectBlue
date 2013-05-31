package jrAccess;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.ClientProtocolException;

import jrFileSystem.IJrItem;
import jrFileSystem.JrFile;
import jrFileSystem.JrFileSystem;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

public class JrSession {
	public static final String PREFS_FILE = "com.lasthopesoftware.jrmediastreamer.PREFS";
	private static final String PLAYLIST_KEY = "playlist";
	private static final String NOW_PLAYING_KEY = "now_playing";
	private static final String NP_POSITION = "np_position";
	private static final String ACCESS_CODE_KEY = "access_code";
	private static final String USER_AUTH_CODE_KEY = "user_auth_code";
	
	public static String UserAuthCode = "";
	public static String AccessCode = "";
	
	public static JrAccessDao accessDao;
    public static ArrayList<IJrItem> categories;
    
    public static IJrItem<?> selectedItem;
    public static JrFile playingFile;
    public static ArrayList<JrFile> playlist;
    
    public static JrFileSystem jrFs;
    
    public static void SaveSession(Context context) {
    	SaveSession(context.getSharedPreferences(PREFS_FILE, 0).edit());
    }
    
    public static void SaveSession(SharedPreferences.Editor prefsEditor) {
    	prefsEditor.putString(ACCESS_CODE_KEY, AccessCode);
    	prefsEditor.putString(USER_AUTH_CODE_KEY, UserAuthCode);
    	
    	LinkedHashSet<String> serializedPlaylist = new LinkedHashSet<String>(playlist.size());
    	for (JrFile file : playlist) 
			serializedPlaylist.add(Integer.toString(file.getKey()));
    	prefsEditor.putStringSet(PLAYLIST_KEY, serializedPlaylist);
		
		if (playingFile != null) {
			prefsEditor.putInt(NOW_PLAYING_KEY, playingFile.getKey());
			prefsEditor.putInt(NP_POSITION, playingFile.getCurrentPosition());
		}
		
		prefsEditor.apply();
    }
    
    public static void CreateSession(Context context) {
    	CreateSession(context.getSharedPreferences(PREFS_FILE, 0));
    }
    
    public static boolean CreateSession(SharedPreferences prefs) {
    	AccessCode = prefs.getString(ACCESS_CODE_KEY, "");
    	UserAuthCode = prefs.getString(USER_AUTH_CODE_KEY, "");
    	
    	if (JrSession.AccessCode == null || JrSession.AccessCode.isEmpty() || !tryConnection()) return false;
    	
    	LinkedHashSet<String> serializedPlaylist = new LinkedHashSet<String>(prefs.getStringSet(PLAYLIST_KEY, new LinkedHashSet<String>()));
    	
    	playlist = new ArrayList<JrFile>(serializedPlaylist.size());
    	for (String id : serializedPlaylist)
    		playlist.add(new JrFile(Integer.parseInt(id)));
    	
    	int nowPlayingFile = prefs.getInt(NOW_PLAYING_KEY, -1);
    	
    	if (nowPlayingFile > -1) {
    		for (JrFile file : playlist) {
    			if (file.getKey() == nowPlayingFile) {
    				playingFile = file;
//    				playingFile.seekTo(prefs.getInt(NP_POSITION, 0));
    			}
    		}
    	}
    	
    	return true;
	}
    
    private static boolean tryConnection() {
    	boolean connectResult = false;
    	try {
			JrSession.accessDao = new GetMcAccess().execute(JrSession.AccessCode).get();
			connectResult = !JrSession.accessDao.getActiveUrl().isEmpty();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return connectResult;
    }
    
    private static class GetMcAccess extends AsyncTask<String, Void, JrAccessDao> {

		@Override
		protected JrAccessDao doInBackground(String... params) {
			
			JrAccessDao accessDao = null;
			// MD5 hash of "vedvicktest" from http://www.md5hashgenerator.com/
			if (params[0].equals("88d0280158de7d924482f909fa199350")) {
				accessDao = new JrAccessDao("ok");
				accessDao.setPort(52199);
				accessDao.setRemoteIp("themachine.dyndns-home.com");
				accessDao.getLocalIps().add("192.168.1.50");
				return accessDao;
			}
	        try {
	        	URLConnection conn = (new URL("http://webplay.jriver.com/libraryserver/lookup?id=" + params[0])).openConnection();
	        	SAXParserFactory parserFactory = SAXParserFactory.newInstance();
	        	SAXParser sp = parserFactory.newSAXParser();
	        	JrLookUpResponseHandler responseHandler = new JrLookUpResponseHandler();
	        	
	        	InputStream mcResponseStream = conn.getInputStream();

	        	sp.parse(mcResponseStream, responseHandler);
	        	
	        	accessDao = responseHandler.getResponse();
	        		
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
	        
	        return accessDao;
		}
    }
}
