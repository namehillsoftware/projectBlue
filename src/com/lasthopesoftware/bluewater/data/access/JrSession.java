package com.lasthopesoftware.bluewater.data.access;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.objects.JrFile;
import com.lasthopesoftware.bluewater.data.objects.JrFileSystem;
import com.lasthopesoftware.bluewater.data.objects.JrItem;
import com.lasthopesoftware.bluewater.data.objects.JrPlaylists;

public class JrSession {
	public static final String PREFS_FILE = "com.lasthopesoftware.jrmediastreamer.PREFS";
	private static final String PLAYLIST_KEY = "Playlist";
	private static final String NOW_PLAYING_KEY = "now_playing";
	private static final String NP_POSITION = "np_position";
	private static final String ACCESS_CODE_KEY = "access_code";
	private static final String USER_AUTH_CODE_KEY = "user_auth_code";
	private static final String IS_LOCAL_ONLY = "is_local_only";
	private static final String LIBRARY_KEY = "library_KEY";

	public static boolean IsLocalOnly = false;

	public static String UserAuthCode = "";
	public static String AccessCode = "";

	public static JrAccessDao accessDao;

	public static int LibraryKey = -1;

	public static IJrItem<?> SelectedItem;
	public static JrFile PlayingFile;
	//    public static ArrayList<JrFile> Playlist;
	public static String Playlist;

	public static JrFileSystem JrFs;

	public static boolean Active = false;

	private static TreeMap<String, IJrItem<?>> mCategories;
	private static ArrayList<IJrItem<?>> mCategoriesList;
	
	private static Object syncObject = new Object();

	public static void SaveSession(Context context) {
		SaveSession(context.getSharedPreferences(PREFS_FILE, 0).edit());
	}

	public static void SaveSession(SharedPreferences.Editor prefsEditor) {
		synchronized(syncObject) {
			prefsEditor.putString(ACCESS_CODE_KEY, AccessCode);
			prefsEditor.putString(USER_AUTH_CODE_KEY, UserAuthCode);
			prefsEditor.putBoolean(IS_LOCAL_ONLY, IsLocalOnly);
			prefsEditor.putInt(LIBRARY_KEY, LibraryKey);
	
			if (Playlist != null) {
				prefsEditor.putString(PLAYLIST_KEY, Playlist);
			}
	
			if (PlayingFile != null) {
				prefsEditor.putInt(NOW_PLAYING_KEY, PlayingFile.getKey());
				prefsEditor.putInt(NP_POSITION, PlayingFile.getCurrentPosition());
			}
	
			prefsEditor.apply();
		}
	}

	public static boolean CreateSession(Context context) {
		return CreateSession(context.getSharedPreferences(PREFS_FILE, 0));
	}

	public static boolean CreateSession(SharedPreferences prefs) {
		synchronized(syncObject) {
			AccessCode = prefs.getString(ACCESS_CODE_KEY, "");
			UserAuthCode = prefs.getString(USER_AUTH_CODE_KEY, "");
			IsLocalOnly = prefs.getBoolean(IS_LOCAL_ONLY, false);
			LibraryKey = prefs.getInt(LIBRARY_KEY, -1);
			Active = false;
			
			if (JrSession.AccessCode == null || JrSession.AccessCode.isEmpty() || !tryConnection()) return false;
	
			Active = true;
	
			try {
				Playlist = prefs.getString(PLAYLIST_KEY, "");
			} catch (ClassCastException ce) {
				Playlist = null;
				return Active;
			}
	
			int savedFileKey = prefs.getInt(NOW_PLAYING_KEY, -1);
			int savedFilePos = prefs.getInt(NP_POSITION, -1);
	
	
			if (savedFileKey < 0) return Active;
			String savedFileKeyString = String.valueOf(savedFileKey);
			for (String fileKey : Playlist.split(";")) {
				if (!savedFileKeyString.equals(fileKey)) continue;
				PlayingFile = new JrFile(savedFileKey);
				if (savedFilePos > -1) PlayingFile.seekTo(savedFilePos);
			}
	
			return Active;
		}
	}

	public static TreeMap<String, IJrItem<?>> getCategories() {
		if (mCategories != null) return mCategories;

		mCategories = new TreeMap<String, IJrItem<?>>();
		for (IJrItem<?> category : getCategoriesList())
			mCategories.put(category.getValue(), category);

		return mCategories;
	}

	public static ArrayList<IJrItem<?>> getCategoriesList() {
		if (mCategoriesList != null) return mCategoriesList;

		if (JrSession.JrFs == null) JrSession.JrFs = new JrFileSystem();

		if (LibraryKey < 0) return null;

		mCategoriesList = new ArrayList<IJrItem<?>>();
		for (JrItem page : JrSession.JrFs.getSubItems()) {
			if (page.getKey() == LibraryKey) {
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
