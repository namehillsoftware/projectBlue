package com.lasthopesoftware.bluewater.data.session;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xmlwise.XmlElement;
import xmlwise.Xmlwise;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.j256.ormlite.dao.Dao;
import com.lasthopesoftware.bluewater.data.service.access.JrAccessDao;
import com.lasthopesoftware.bluewater.data.service.objects.JrFileSystem;
import com.lasthopesoftware.bluewater.data.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.data.sqlite.objects.SavedTrack;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class JrSession {
	public static final String PREFS_FILE = "com.lasthopesoftware.jrmediastreamer.PREFS";
//	private static final String PLAYLIST_KEY = "Playlist";
//	private static final String NOW_PLAYING_KEY = "now_playing";
//	private static final String NP_POSITION = "np_position";
//	private static final String ACCESS_CODE_KEY = "access_code";
//	private static final String USER_AUTH_CODE_KEY = "user_auth_code";
//	private static final String IS_LOCAL_ONLY = "is_local_only";
//	private static final String LIBRARY_KEY = "library_KEY";
	private static final String CHOSEN_LIBRARY = "chosen_library";
	
	public static int ChosenLibrary = -1;

//	public static boolean IsLocalOnly = false;
//
//	public static String UserAuthCode = "";
//	public static String AccessCode = "";

	public static JrAccessDao accessDao;

//	private static int[] SelectedViewIds = new int[0];
//
//	public static IJrItem<?> SelectedItem;
//	public static JrFile PlayingFile;
//	//    public static ArrayList<JrFile> Playlist;
//	public static String Playlist;
	
	private static ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
	
	private static Library library = null;

	public static JrFileSystem JrFs;

	private static boolean mActive = false;
	
	public static void SaveSession(Context context) {
		SaveSession(context, null);
	}

	public static void SaveSession(Context context, OnCompleteListener<Void, Void, Library> onSaveComplete) { 
		
		if (library == null) library = new Library();
		
		final Context _context = context;
		SimpleTask<Void, Void, Library> writeToDatabaseTask = new SimpleTask<Void, Void, Library>();
		writeToDatabaseTask.addOnExecuteListener(new OnExecuteListener<Void, Void, Library>() {
			
			@Override
			public void onExecute(ISimpleTask<Void, Void, Library> owner, Void... params) throws Exception {
				DatabaseHandler handler = new DatabaseHandler(_context);
				try {
					Dao<Library, Integer> libraryAccess = handler.getAccessObject(Library.class);
					Dao<SavedTrack, Integer> savedTrackAccess = handler.getAccessObject(SavedTrack.class);
					Library oldLibrary = libraryAccess.queryForId(library.getId());
					if (oldLibrary != null) {
						savedTrackAccess.delete(oldLibrary.getSavedTracks());
					}
					libraryAccess.createOrUpdate(library);
					ChosenLibrary = library.getId();
					_context.getSharedPreferences(PREFS_FILE, 0).edit().putInt(CHOSEN_LIBRARY, library.getId()).apply();
					
					for (SavedTrack savedTrack : library.getSavedTracks())
						savedTrackAccess.create(savedTrack);
					
					owner.setResult(library);
				} catch (SQLException e) {
					LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
				} catch (Exception e) {
					LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
				} finally {
					handler.close();
				}
				
				Logger log = LoggerFactory.getLogger(JrSession.class);
				log.info("Session saved.");
			}
		});
		if (onSaveComplete != null)
			writeToDatabaseTask.addOnCompleteListener(onSaveComplete);
		
		writeToDatabaseTask.executeOnExecutor(databaseExecutor);
	}
	
	public static synchronized Library GetLibrary() throws Exception {
		if (library == null)
			throw new Exception("The library has not been initialized correctly. Please call GetLibrary(Context context)) first.");
		
		return library;
	}

	public static synchronized Library GetLibrary(Context context) {
		if (library != null) return library;
		
		mActive = false;
		library = new Library();
		
		
		ChosenLibrary = context.getSharedPreferences(PREFS_FILE, 0).getInt(CHOSEN_LIBRARY, -1);
		
		if (ChosenLibrary < 0) return library;
		final Context _context = context;
		SimpleTask<Integer, Void, Library> getLibraryTask = new SimpleTask<Integer, Void, Library>();
		getLibraryTask.addOnExecuteListener(new OnExecuteListener<Integer, Void, Library>() {
			
			@Override
			public void onExecute(ISimpleTask<Integer, Void, Library> owner, Integer... params) throws Exception {
				DatabaseHandler handler = new DatabaseHandler(_context);
				try {
					Dao<Library, Integer> libraryAccess = handler.getAccessObject(Library.class);
					owner.setResult(libraryAccess.queryForId(params[0]));
				} catch (SQLException e) {
					LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
				} catch (Exception e) {
					LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
				} finally {
					handler.close();
				}
			}
		});
		
		try {
			library = getLibraryTask.executeOnExecutor(databaseExecutor, ChosenLibrary).get();
		} catch (InterruptedException e) {
			LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
		} catch (ExecutionException e) {
			LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
		}
		
		if (library != null && library.getAccessCode() != null && !library.getAccessCode().isEmpty() && tryConnection(library.getAccessCode())) {
			JrSession.JrFs = new JrFileSystem(library.getSelectedView());
			mActive = true;
		}
		
		
		Logger log = LoggerFactory.getLogger(JrSession.class);
		log.info("Session started.");
		
		return library;
	}
		
	public synchronized static Library ChooseLibrary(Context context, int libraryKey) {
		context.getSharedPreferences(PREFS_FILE, 0).edit().putInt(CHOSEN_LIBRARY, libraryKey).apply();
		
		library = null;
		
		return GetLibrary(context);
	}
	
	public static boolean isActive() {
		return mActive;
	}

	private static boolean tryConnection(String accessCode) {
		boolean connectResult = false;
		try {
			JrSession.accessDao = new GetMcAccess().execute(accessCode).get();
			connectResult = !JrSession.accessDao.getActiveUrl().isEmpty();
		} catch (InterruptedException e) {
			LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
		} catch (ExecutionException e) {
			LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
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
					library.setLocalOnly(false);
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
				LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
			} catch (IOException e) {
				LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
			} catch (Exception e) {
				LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
			}

			return accessDao;
		}
	}
}
