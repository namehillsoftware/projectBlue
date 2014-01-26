package com.lasthopesoftware.bluewater.data.session;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xmlwise.XmlElement;
import xmlwise.Xmlwise;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;

import com.j256.ormlite.dao.Dao;
import com.lasthopesoftware.bluewater.data.service.access.JrAccessDao;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.service.objects.JrFileSystem;
import com.lasthopesoftware.bluewater.data.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.data.sqlite.objects.SavedTrack;
import com.lasthopesoftware.bluewater.data.sqlite.objects.View;

public class JrSession {
	public static final String PREFS_FILE = "com.lasthopesoftware.jrmediastreamer.PREFS";
	private static final String PLAYLIST_KEY = "Playlist";
	private static final String NOW_PLAYING_KEY = "now_playing";
	private static final String NP_POSITION = "np_position";
	private static final String ACCESS_CODE_KEY = "access_code";
	private static final String USER_AUTH_CODE_KEY = "user_auth_code";
	private static final String IS_LOCAL_ONLY = "is_local_only";
	private static final String LIBRARY_KEY = "library_KEY";
	private static final String CHOSEN_LIBRARY = "chosen_library";
	
	public static int ChosenLibrary = -1;

	public static boolean IsLocalOnly = false;

	public static String UserAuthCode = "";
	public static String AccessCode = "";

	public static JrAccessDao accessDao;

	private static int[] SelectedViewIds = new int[0];

	public static IJrItem<?> SelectedItem;
	public static JrFile PlayingFile;
	//    public static ArrayList<JrFile> Playlist;
	public static String Playlist;

	public static JrFileSystem JrFs;

	private static boolean mActive = false;

	public static void SaveSession(Context context) {
		SaveSession(context.getSharedPreferences(PREFS_FILE, 0).edit());
	}

	public static void SaveSession(SharedPreferences.Editor prefsEditor) {
		prefsEditor.putString(ACCESS_CODE_KEY, AccessCode);
		prefsEditor.putString(USER_AUTH_CODE_KEY, UserAuthCode);
		prefsEditor.putBoolean(IS_LOCAL_ONLY, IsLocalOnly);
		prefsEditor.putStringSet(LIBRARY_KEY, getLibraryKeysSet());
		
		if (Playlist != null) {
			prefsEditor.putString(PLAYLIST_KEY, Playlist);
		}

		if (PlayingFile != null) {
			prefsEditor.putInt(NOW_PLAYING_KEY, PlayingFile.getKey());
			prefsEditor.putInt(NP_POSITION, PlayingFile.getCurrentPosition());
		}

		prefsEditor.apply();
		
		Logger log = LoggerFactory.getLogger(JrSession.class);
		log.info("Session saved.");
	}

	public static boolean CreateSession(Context context) {
		
		Logger log = LoggerFactory.getLogger(JrSession.class);
		log.info("Session started.");
		
		DatabaseHandler handler = new DatabaseHandler(context);
		try {
			ChosenLibrary = context.getSharedPreferences(PREFS_FILE, 0).getInt(CHOSEN_LIBRARY, -1);
			
			if (ChosenLibrary < -1) return false;
			
			Dao<Library, Integer> libraryAccess = handler.getAccessObject(Library.class);
			
			Library library = libraryAccess.queryForId(ChosenLibrary);
			AccessCode = library.getAccessCode();
			UserAuthCode = library.getAuthKey();
			IsLocalOnly = library.isLocalOnly();
			setLibraryIds(library.getViews());
			mActive = false;
			
			if (JrSession.AccessCode == null || JrSession.AccessCode.isEmpty() || !tryConnection()) return false;
			
			if (JrSession.JrFs == null) JrSession.JrFs = new JrFileSystem(getLibraryKeys());
			mActive = true;
		
			int savedFileKey = library.getNowPlayingId();
			int savedFilePos = library.getNowPlayingProgress();
	
	
			if (savedFileKey < 0) return mActive;
			
			for (SavedTrack track : library.getSavedTracks()) {
				if (track.getTrackId() != savedFileKey) continue;
				PlayingFile = new JrFile(savedFileKey);
				if (savedFilePos > -1) PlayingFile.seekTo(savedFilePos);
			}
	
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			handler.close();
		}
		
		return mActive;
	}
	
	public static void setLibraryIds(int[] keys) {
		SelectedViewIds = keys;
	}
	
	public static void setLibraryIds(Collection<View> views) {
		int i = 0;
		SelectedViewIds = new int[views.size()];
		for (View view : views)
			SelectedViewIds[i++] = view.getId();
	}
	
	public static boolean isActive() {
		return mActive;
	}
	
	public static int[] getLibraryKeys() {
		return SelectedViewIds;
	}
	
	public static HashSet<String> getLibraryKeysSet() {
		HashSet<String> libraryKeys = new HashSet<String>(SelectedViewIds.length);
		for (int key : SelectedViewIds)
			libraryKeys.add(String.valueOf(key));
		
		return libraryKeys;
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
				accessDao = new JrAccessDao();
				
				if (UrlValidator.getInstance().isValid(params[0])) {
					Uri jrUrl = Uri.parse(params[0]);
					accessDao.setRemoteIp(jrUrl.getHost());
					accessDao.setPort(jrUrl.getPort());
					accessDao.setStatus(true);
					IsLocalOnly = false;
				} else {
					URLConnection conn = (new URL("http://webplay.jriver.com/libraryserver/lookup?id=" + params[0])).openConnection();
					XmlElement xml = Xmlwise.createXml(IOUtils.toString(conn.getInputStream()));
					
					
					accessDao.setStatus(xml.getAttribute("Status").equalsIgnoreCase("OK"));
					accessDao.setPort(Integer.parseInt(xml.getUnique("port").getValue()));
					accessDao.setRemoteIp(xml.getUnique("ip").getValue());
					for (String localIp : xml.getUnique("localiplist").getValue().split(","))
						accessDao.getLocalIps().add(localIp);
					for (String macAddress : xml.getUnique("macaddresslist").getValue().split(","))
						accessDao.getMacAddresses().add(macAddress);
				}
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
