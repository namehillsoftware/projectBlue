package com.lasthopesoftware.bluewater.disk.sqlite.access;

import android.content.Context;
import android.content.SharedPreferences;

import com.j256.ormlite.dao.Dao;
import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LibrarySession {
	
	private static final Logger mLogger = LoggerFactory.getLogger(LibrarySession.class);
	
	private static final String CHOSEN_LIBRARY = "chosen_library";
	
	public static void SaveLibrary(final Context context, final Library library) {
		SaveLibrary(context, library, null);
	}
	
	public static void SaveLibrary(final Context context, final Library library, final OnCompleteListener<Void, Void, Library> onSaveComplete) { 
	
		final SimpleTask<Void, Void, Library> writeToDatabaseTask = new SimpleTask<>(new OnExecuteListener<Void, Void, Library>() {
			
			@Override
			public Library onExecute(ISimpleTask<Void, Void, Library> owner, Void... params) throws Exception {
				final DatabaseHandler handler = new DatabaseHandler(context);
				try {
					final Dao<Library, Integer> libraryAccess = handler.getAccessObject(Library.class);
					
					libraryAccess.createOrUpdate(library);
					context.getSharedPreferences(ApplicationConstants.PREFS_FILE, 0).edit().putInt(CHOSEN_LIBRARY, library.getId()).apply();
					
					mLogger.debug("Session saved.");
					return library;
				} catch (SQLException e) {
					mLogger.error(e.toString(), e);
				} catch (Exception e) {
					mLogger.error(e.toString(), e);
				} finally {
					handler.close();
				}
				
				return null;
			}
		});
		
		if (onSaveComplete != null)
			writeToDatabaseTask.addOnCompleteListener(onSaveComplete);
		
		writeToDatabaseTask.execute(DatabaseHandler.databaseExecutor);
	}
	
	public static void GetLibrary(final Context context, final OnCompleteListener<Integer, Void, Library> onGetLibraryComplete) {
		
		final SimpleTask<Integer, Void, Library> getLibraryTask = new SimpleTask<>(new OnExecuteListener<Integer, Void, Library>() {
			
			@Override
			public Library onExecute(ISimpleTask<Integer, Void, Library> owner, Integer... params) throws Exception {
				return GetLibrary(context);
			}
		});
		
		getLibraryTask.addOnCompleteListener(new OnCompleteListener<Integer, Void, Library>() {
			
			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				if (onGetLibraryComplete != null)
					onGetLibraryComplete.onComplete(owner, result);
			}
		});
		
		getLibraryTask.execute(DatabaseHandler.databaseExecutor);
	}
	
	public static synchronized Library GetLibrary(final Context context) {
		if ("Main".equals(Thread.currentThread().getName()))
			throw new IllegalStateException("This method must be called from a background thread.");
		
		int chosenLibrary = context.getSharedPreferences(ApplicationConstants.PREFS_FILE, 0).getInt(CHOSEN_LIBRARY, -1);
		
		if (chosenLibrary < 0) return null;
		
		final DatabaseHandler handler = new DatabaseHandler(context);
		try {
			final Dao<Library, Integer> libraryAccess = handler.getAccessObject(Library.class);
			return libraryAccess.queryForId(chosenLibrary);
		} catch (SQLException e) {
			mLogger.error(e.toString(), e);
		} catch (Exception e) {
			mLogger.error(e.toString(), e);
		} finally {
			handler.close();
		}
		
		return null;
	}
	
	public static List<Library> GetLibraries(Context context) {
		final Context _context = context;
		SimpleTask<Void, Void, List<Library>> getLibrariesTask = new SimpleTask<>(new OnExecuteListener<Void, Void, List<Library>>() {
			
			@Override
			public List<Library> onExecute(ISimpleTask<Void, Void, List<Library>> owner, Void... params) throws Exception {
				DatabaseHandler handler = new DatabaseHandler(_context);
				try {
					return handler.getAccessObject(Library.class).queryForAll();
				} catch (SQLException e) {
					mLogger.error(e.toString(), e);
				} catch (Exception e) {
					mLogger.error(e.toString(), e);
				} finally {
					handler.close();
				}
				
				return new ArrayList<>();
			}
		});
		
		try {
			return getLibrariesTask.execute(DatabaseHandler.databaseExecutor).get();
		} catch (Exception e) {
			mLogger.error(e.toString(), e);
		}
		
		// Exceptions occurred, return an empty mLibrary
		return new ArrayList<>();
	}
		
	public synchronized static void ChooseLibrary(Context context, int libraryKey, final OnCompleteListener<Integer, Void, Library> onLibraryChangeComplete) {

        final SharedPreferences sharedPreferences = context.getSharedPreferences(ApplicationConstants.PREFS_FILE, 0);
		if (libraryKey != sharedPreferences.getInt(CHOSEN_LIBRARY, -1)) {
            sharedPreferences.edit().putInt(CHOSEN_LIBRARY, libraryKey).apply();
		}
		
		GetLibrary(context, onLibraryChangeComplete);
	}
}
