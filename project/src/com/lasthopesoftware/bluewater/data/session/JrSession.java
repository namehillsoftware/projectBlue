package com.lasthopesoftware.bluewater.data.session;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.LoggerFactory;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.lasthopesoftware.bluewater.data.service.objects.FileSystem;
import com.lasthopesoftware.bluewater.data.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class JrSession {
	public static final String PREFS_FILE = "com.lasthopesoftware.jrmediastreamer.PREFS";
	private static final String CHOSEN_LIBRARY = "chosen_library";
	public static int ChosenLibrary = -1;

	private static ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
	
	private static Library mLibrary = null;

	public static FileSystem JrFs;
	
	public static void SaveSession(final Context context) {
		SaveSession(context, null);
	}

	public static void SaveSession(final Context context, final OnCompleteListener<Void, Void, Library> onSaveComplete) {
		SaveSession(context, mLibrary, onSaveComplete);
	}
	
	public static void SaveSession(final Context context, final Library library, final OnCompleteListener<Void, Void, Library> onSaveComplete) { 
	
		final Context _context = context;
		SimpleTask<Void, Void, Library> writeToDatabaseTask = new SimpleTask<Void, Void, Library>();
		writeToDatabaseTask.setOnExecuteListener(new OnExecuteListener<Void, Void, Library>() {
			
			@Override
			public Library onExecute(ISimpleTask<Void, Void, Library> owner, Void... params) throws Exception {
				DatabaseHandler handler = new DatabaseHandler(_context);
				try {
					Dao<Library, Integer> libraryAccess = handler.getAccessObject(Library.class);
					
					libraryAccess.createOrUpdate(library);
					ChosenLibrary = library.getId();
					_context.getSharedPreferences(PREFS_FILE, 0).edit().putInt(CHOSEN_LIBRARY, library.getId()).apply();
					
					LoggerFactory.getLogger(JrSession.class).debug("Session saved.");
					return library;
				} catch (SQLException e) {
					LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
				} catch (Exception e) {
					LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
				} finally {
					handler.close();
				}
				
				return null;
			}
		});
		
		if (onSaveComplete != null)
			writeToDatabaseTask.addOnCompleteListener(onSaveComplete);
		
		writeToDatabaseTask.executeOnExecutor(databaseExecutor);
	}
	
	public static synchronized Library GetLibrary() throws NullPointerException {
		if (mLibrary == null)
			throw new NullPointerException("The mLibrary has not been initialized correctly. Please call GetLibrary(Context context)) first.");
		
		return mLibrary;
	}

	public static synchronized void GetLibrary(final Context context, final OnCompleteListener<Integer, Void, Library> onGetLibraryComplete) {
		
		final SimpleTask<Integer, Void, Library> getLibraryTask = new SimpleTask<Integer, Void, Library>();
		getLibraryTask.setOnExecuteListener(new OnExecuteListener<Integer, Void, Library>() {
			
			@Override
			public Library onExecute(ISimpleTask<Integer, Void, Library> owner, Integer... params) throws Exception {
				if (mLibrary != null) {
					return mLibrary;
				}
						
				ChosenLibrary = context.getSharedPreferences(PREFS_FILE, 0).getInt(CHOSEN_LIBRARY, -1);
				
				if (ChosenLibrary < 0) return null;
				
				DatabaseHandler handler = new DatabaseHandler(context);
				try {
					Dao<Library, Integer> libraryAccess = handler.getAccessObject(Library.class);
					return libraryAccess.queryForId(ChosenLibrary);
				} catch (SQLException e) {
					LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
				} catch (Exception e) {
					LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
				} finally {
					handler.close();
				}
				
				return null;
			}
		});
		
		getLibraryTask.addOnCompleteListener(new OnCompleteListener<Integer, Void, Library>() {
			
			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				mLibrary = result;
				if (onGetLibraryComplete != null)
					onGetLibraryComplete.onComplete(owner, result);
			}
		});
		
		getLibraryTask.executeOnExecutor(databaseExecutor);
	}
	
	public static List<Library> GetLibraries(Context context) {
		final Context _context = context;
		SimpleTask<Void, Void, List<Library>> getLibrariesTask = new SimpleTask<Void, Void, List<Library>>();
		getLibrariesTask.setOnExecuteListener(new OnExecuteListener<Void, Void, List<Library>>() {
			
			@Override
			public List<Library> onExecute(ISimpleTask<Void, Void, List<Library>> owner, Void... params) throws Exception {
				DatabaseHandler handler = new DatabaseHandler(_context);
				try {
					return handler.getAccessObject(Library.class).queryForAll();
				} catch (SQLException e) {
					LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
				} catch (Exception e) {
					LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
				} finally {
					handler.close();
				}
				
				return new ArrayList<Library>();
			}
		});
		
		try {
			return getLibrariesTask.executeOnExecutor(databaseExecutor).get();
		} catch (InterruptedException e) {
			LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
		} catch (ExecutionException e) {
			LoggerFactory.getLogger(JrSession.class).error(e.toString(), e);
		}
		
		// Exceptions occurred, return an empty mLibrary
		return new ArrayList<Library>();
	}
		
	public synchronized static void ChooseLibrary(Context context, int libraryKey, final OnCompleteListener<Integer, Void, Library> onLibraryChangeComplete) {
		
		if (libraryKey != context.getSharedPreferences(PREFS_FILE, 0).getInt(CHOSEN_LIBRARY, -1)) {
			context.getSharedPreferences(PREFS_FILE, 0).edit().putInt(CHOSEN_LIBRARY, libraryKey).apply();
			mLibrary = null;
		}
		
		GetLibrary(context, onLibraryChangeComplete);
	}
}
