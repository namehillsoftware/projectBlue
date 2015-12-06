package com.lasthopesoftware.bluewater.servers.library.repository;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.j256.ormlite.dao.Dao;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.shared.SpecialValueHelpers;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;
import com.lasthopesoftware.threading.FluentTask;
import com.lasthopesoftware.threading.OnExecuteListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LibrarySession {
	
	private static final Logger logger = LoggerFactory.getLogger(LibrarySession.class);
	public static final String libraryChosenEvent = SpecialValueHelpers.buildMagicPropertyName(LibrarySession.class, "libraryChosenEvent");
	public static final String chosenLibraryInt = "chosen_library";

	public static void SaveLibrary(final Context context, final Library library) {
		SaveLibrary(context, library, null);
	}

	public static void SaveLibrary(final Context context, final Library library, final ITwoParameterRunnable<FluentTask<Void, Void, Library>, Library> onSaveComplete) {

		final FluentTask<Void, Void, Library> writeToDatabaseTask = new FluentTask<>(new OnExecuteListener<Void, Void, Library>() {

			@Override
			public Library onExecute(FluentTask<Void, Void, Library> owner, Void... params) throws Exception {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					repositoryAccessHelper.getDataAccess(Library.class).createOrUpdate(library);
					logger.debug("Library saved.");
					return library;
				} catch (SQLException e) {
					logger.error(e.toString(), e);
				} finally {
					repositoryAccessHelper.close();
				}

				return null;
			}
		});

		if (onSaveComplete != null)
			writeToDatabaseTask.onComplete(onSaveComplete);

		writeToDatabaseTask.execute(RepositoryAccessHelper.databaseExecutor);
	}

	public static void GetActiveLibrary(final Context context, final ITwoParameterRunnable<FluentTask<Integer, Void, Library>, Library> onGetLibraryComplete) {
		ExecuteGetLibrary(new FluentTask<>(new OnExecuteListener<Integer, Void, Library>() {

			@Override
			public Library onExecute(FluentTask<Integer, Void, Library> owner, Integer... params) throws Exception {
				return GetActiveLibrary(context);
			}
		}), onGetLibraryComplete);
	}

	public static void GetLibrary(final Context context, final int libraryId, final ITwoParameterRunnable<FluentTask<Integer, Void, Library>, Library> onGetLibraryComplete) {
		ExecuteGetLibrary(new FluentTask<>(new OnExecuteListener<Integer, Void, Library>() {

			@Override
			public Library onExecute(FluentTask<Integer, Void, Library> owner, Integer... params) throws Exception {
				return GetLibrary(context, libraryId);
			}
		}), onGetLibraryComplete);
	}

	private static void ExecuteGetLibrary(FluentTask<Integer, Void, Library> getLibraryTask, final ITwoParameterRunnable<FluentTask<Integer, Void, Library>, Library> onGetLibraryComplete) {

		getLibraryTask.onComplete(new ITwoParameterRunnable<FluentTask<Integer, Void, Library>, Library>() {

			@Override
			public void run(FluentTask<Integer, Void, Library> owner, Library result) {
				if (onGetLibraryComplete != null)
					onGetLibraryComplete.run(owner, result);
			}
		});

		getLibraryTask.execute(RepositoryAccessHelper.databaseExecutor);
	}

	public static synchronized Library GetActiveLibrary(final Context context) {
		if ("Main".equals(Thread.currentThread().getName()))
			throw new IllegalStateException("This method must be called from a background thread.");

		final int chosenLibraryId = PreferenceManager.getDefaultSharedPreferences(context).getInt(chosenLibraryInt, -1);
		return chosenLibraryId >= 0 ? GetLibrary(context, chosenLibraryId) : null;
	}

	private static synchronized Library GetLibrary(final Context context, int libraryId) {
		if (libraryId < 0) return null;

		final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
		try {
			final Dao<Library, Integer> libraryAccess = repositoryAccessHelper.getDataAccess(Library.class);
			return libraryAccess.queryForId(libraryId);
		} catch (SQLException e) {
			logger.error(e.toString(), e);
		} finally {
			repositoryAccessHelper.close();
		}

		return null;
	}
	
	public static void GetLibraries(final Context context, ITwoParameterRunnable<FluentTask<Void, Void, List<Library>>, List<Library>> onGetLibrariesComplete) {
		final FluentTask<Void, Void, List<Library>> getLibrariesTask = new FluentTask<>(new OnExecuteListener<Void, Void, List<Library>>() {
			
			@Override
			public List<Library> onExecute(FluentTask<Void, Void, List<Library>> owner, Void... params) throws Exception {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					return repositoryAccessHelper.getDataAccess(Library.class).queryForAll();
				} catch (SQLException e) {
					logger.error(e.toString(), e);
				} finally {
					repositoryAccessHelper.close();
				}
				
				return new ArrayList<>();
			}
		});

		if (onGetLibrariesComplete != null)
			getLibrariesTask.onComplete(onGetLibrariesComplete);

		getLibrariesTask.execute(RepositoryAccessHelper.databaseExecutor);
	}
		
	public synchronized static void ChooseLibrary(final Context context, final int libraryKey, final ITwoParameterRunnable<FluentTask<Integer, Void, Library>, Library> onLibraryChangeComplete) {

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (libraryKey != sharedPreferences.getInt(chosenLibraryInt, -1)) {
            sharedPreferences.edit().putInt(chosenLibraryInt, libraryKey).apply();
		}
		
		GetActiveLibrary(context, new ITwoParameterRunnable<FluentTask<Integer, Void, Library>, Library>() {
			@Override
			public void run(FluentTask<Integer, Void, Library> owner, Library library) {
				final Intent broadcastIntent = new Intent(libraryChosenEvent);
				broadcastIntent.putExtra(chosenLibraryInt, libraryKey);
				LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);

				if (onLibraryChangeComplete != null)
					onLibraryChangeComplete.run(owner, library);
			}
		});
	}
}
