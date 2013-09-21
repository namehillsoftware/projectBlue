package com.lasthopesoftware.bluewater.access;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.ClientProtocolException;

import com.lasthopesoftware.bluewater.FileSystem.IJrItem;
import com.lasthopesoftware.bluewater.FileSystem.JrFile;
import com.lasthopesoftware.bluewater.FileSystem.JrFileSystem;
import com.lasthopesoftware.bluewater.FileSystem.JrItem;
import com.lasthopesoftware.bluewater.FileSystem.JrPlaylists;

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
	private static final String IS_LOCAL_ONLY = "is_local_only";
	
	public static boolean IsLocalOnly = false;
	
	public static String UserAuthCode = "";
	public static String AccessCode = "";
	
	public static JrAccessDao accessDao;
	
    private static TreeMap<String, IJrItem> mCategories;
    private static ArrayList<IJrItem> mCategoriesList;
    
    public static IJrItem<?> selectedItem;
    public static JrFile playingFile;
    public static ArrayList<JrFile> playlist;
    
    public static JrFileSystem jrFs;
    
    public static boolean Active = false;
    
    public static void SaveSession(Context context) {
    	SaveSession(context.getSharedPreferences(PREFS_FILE, 0).edit());
    }
    
    public static void SaveSession(SharedPreferences.Editor prefsEditor) {
    	prefsEditor.putString(ACCESS_CODE_KEY, AccessCode);
    	prefsEditor.putString(USER_AUTH_CODE_KEY, UserAuthCode);
    	prefsEditor.putBoolean(IS_LOCAL_ONLY, IsLocalOnly);
    	
    	if (playlist != null) {
	    	LinkedHashSet<String> serializedPlaylist = new LinkedHashSet<String>(playlist.size());
	    	for (JrFile file : playlist) 
				serializedPlaylist.add(Integer.toString(file.getKey()));
	    	prefsEditor.putStringSet(PLAYLIST_KEY, serializedPlaylist);
    	}
		
		if (playingFile != null) {
			prefsEditor.putInt(NOW_PLAYING_KEY, playingFile.getKey());
			if (playingFile.getMediaPlayer() != null) prefsEditor.putInt(NP_POSITION, playingFile.getCurrentPosition());
		}
		
		prefsEditor.apply();
    }
    
    public static void CreateSession(Context context) {
    	CreateSession(context.getSharedPreferences(PREFS_FILE, 0));
    }
    
    public static boolean CreateSession(SharedPreferences prefs) {
    	AccessCode = prefs.getString(ACCESS_CODE_KEY, "");
    	UserAuthCode = prefs.getString(USER_AUTH_CODE_KEY, "");
    	IsLocalOnly = prefs.getBoolean(IS_LOCAL_ONLY, false);
    	
    	if (JrSession.AccessCode == null || JrSession.AccessCode.isEmpty() || !tryConnection()) return false;
    	
    	LinkedHashSet<String> serializedPlaylist = new LinkedHashSet<String>(prefs.getStringSet(PLAYLIST_KEY, new LinkedHashSet<String>()));
    	
    	Integer[] params = new Integer[serializedPlaylist.size()];
    	int i = 0;
    	for (String id : serializedPlaylist) {
    		params[i++] = Integer.parseInt(id);
    	}
    	
    	new RebuildPlaylist(prefs.getInt(NOW_PLAYING_KEY, -1)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
    	
    	Active = true;
    	return true;
	}
    
    public static TreeMap<String, IJrItem> getCategories() {
    	if (mCategories != null) return mCategories;
    	
    	mCategories = new TreeMap<String, IJrItem>();
		for (IJrItem category : getCategoriesList())
			mCategories.put(category.getValue(), category);
		
		return mCategories;
    }
    
    public static ArrayList<IJrItem> getCategoriesList() {
    	if (mCategoriesList != null) return mCategoriesList;
    	
		if (JrSession.jrFs == null) JrSession.jrFs = new JrFileSystem();
    	
    	mCategoriesList = new ArrayList<IJrItem>();
    	for (JrItem page : JrSession.jrFs.getSubItems()) {
			if (page.getKey() == 1) {
				mCategoriesList = ((IJrItem) page).getSubItems();
				break;
			}
		}
		
    	JrPlaylists playlists = new JrPlaylists(mCategoriesList.size());
    	mCategoriesList.add(playlists);
    	
    	return mCategoriesList;
    }
    
    private static boolean tryConnection() {
    	boolean connectResult = false;
    	try {
			JrSession.accessDao = new GetMcAccess().execute(JrSession.AccessCode).get();
			connectResult = !JrSession.accessDao.getActiveUrl().isEmpty();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
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
    
    private static class RebuildPlaylist extends AsyncTask<Integer, Void, ArrayList<JrFile>> {
    	int mNowPlayingFieldId;
    	
    	public RebuildPlaylist(int nowPlayingFieldId) {
    		super();
    		mNowPlayingFieldId = nowPlayingFieldId;
    	}
    	
		@Override
		protected ArrayList<JrFile> doInBackground(Integer... params) {
			ArrayList<JrFile> recoveredPlaylist = new ArrayList<JrFile>(params.length);
	    	for (int i = 0; i < params.length; i++) {
	    		JrFile recoveredFile = new JrFile(params[i]); 
	    		recoveredPlaylist.add(recoveredFile);
	    		
	    		if (i > 0) {
	    			recoveredFile.setPreviousFile(recoveredPlaylist.get(i - 1));
	    			recoveredPlaylist.get(i - 1).setNextFile(recoveredFile);
	    		}
	    	}
	    	
	    	// die if the playlist is not null already
			if (playlist != null) return recoveredPlaylist;
			playlist = recoveredPlaylist;
			
	    	if (mNowPlayingFieldId < 0) return recoveredPlaylist;
	    	
    		for (JrFile file : playlist) {
    			if (file.getKey() == mNowPlayingFieldId) playingFile = file;
    		}
	    	
	    	return recoveredPlaylist;
		}
    }
}
