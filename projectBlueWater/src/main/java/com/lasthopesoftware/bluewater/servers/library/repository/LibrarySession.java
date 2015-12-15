package com.lasthopesoftware.bluewater.servers.library.repository;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.shared.SpecialValueHelpers;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;
import com.lasthopesoftware.threading.FluentTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LibrarySession {
	
	private static final Logger logger = LoggerFactory.getLogger(LibrarySession.class);
	public static final String libraryChosenEvent = SpecialValueHelpers.buildMagicPropertyName(LibrarySession.class, "libraryChosenEvent");
	public static final String chosenLibraryInt = "chosen_library";

	public static void SaveLibrary(final Context context, final Library library) {
		SaveLibrary(context, library, null);
	}

	public static void SaveLibrary(final Context context, final Library library, final ITwoParameterRunnable<FluentTask<Void, Void, Library>, Library> onSaveComplete) {
		final String libraryUpdateSql =
				" UPDATE " + Library.tableName +
				" SET " + Library.accessCodeColumn + " = :" + Library.accessCodeColumn +
				", " + Library.authKeyColumn + " = :" + Library.authKeyColumn +
				", " + Library.isLocalOnlyColumn + " = :" + Library.isLocalOnlyColumn +
				", " + Library.libraryNameColumn + " + :" + Library.libraryNameColumn +
				", " + Library.isRepeatingColumn + " + :" + Library.isRepeatingColumn +
				", " + Library.customSyncedFilesPathColumn + " + :" + Library.customSyncedFilesPathColumn +
				", " + Library.isSyncLocalConnectionsOnlyColumn + " + :" + Library.isSyncLocalConnectionsOnlyColumn +
				", " + Library.isUsingExistingFilesColumn + " + :" + Library.isUsingExistingFilesColumn +
				", " + Library.nowPlayingIdColumn + " + :" + Library.nowPlayingIdColumn +
				", " + Library.nowPlayingProgressColumn + " + :" + Library.nowPlayingProgressColumn +
				", " + Library.savedTracksStringColumn + " + :" + Library.savedTracksStringColumn +
				", " + Library.selectedViewColumn + " + :" + Library.selectedViewColumn +
				", " + Library.selectedViewTypeColumn + " + :" + Library.selectedViewTypeColumn +
				", " + Library.syncedFileLocationColumn + " + :" + Library.syncedFileLocationColumn +
				" WHERE id = :id";

		final FluentTask<Void, Void, Library> writeToDatabaseTask = new FluentTask<Void, Void, Library>() {

			@Override
			protected Library executeInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					repositoryAccessHelper
							.mapSql(libraryUpdateSql)
							.addParameter(Library.accessCodeColumn, library.getAccessCode())
							.addParameter(Library.authKeyColumn, library.getAuthKey())
							.addParameter(Library.isLocalOnlyColumn, library.isLocalOnly())
							.addParameter(Library.libraryNameColumn, library.getLibraryName())
							.addParameter(Library.isRepeatingColumn, library.isRepeating())
							.addParameter(Library.customSyncedFilesPathColumn, library.getCustomSyncedFilesPath())
							.addParameter(Library.isSyncLocalConnectionsOnlyColumn, library.isSyncLocalConnectionsOnly())
							.addParameter(Library.isUsingExistingFilesColumn, library.isUsingExistingFiles())
							.addParameter(Library.nowPlayingIdColumn, library.getNowPlayingId())
							.addParameter(Library.nowPlayingProgressColumn, library.getNowPlayingProgress())
							.addParameter(Library.savedTracksStringColumn, library.getSavedTracksString())
							.addParameter(Library.selectedViewColumn, library.getSelectedView())
							.addParameter(Library.selectedViewTypeColumn, library.getSelectedViewType())
							.addParameter(Library.syncedFileLocationColumn, library.getSyncedFileLocation())
							.execute();

					logger.debug("Library saved.");
					return library;
				} finally {
					repositoryAccessHelper.close();
				}
			}
		};

		if (onSaveComplete != null)
			writeToDatabaseTask.onComplete(onSaveComplete);

		writeToDatabaseTask.execute(RepositoryAccessHelper.databaseExecutor);
	}

	public static void GetActiveLibrary(final Context context, final ITwoParameterRunnable<FluentTask<Integer, Void, Library>, Library> onGetLibraryComplete) {
		ExecuteGetLibrary(new FluentTask<Integer, Void, Library>() {
			@Override
			protected Library executeInBackground(Integer... params) {
				return GetActiveLibrary(context);
			}
		}, onGetLibraryComplete);
	}

	public static void GetLibrary(final Context context, final int libraryId, final ITwoParameterRunnable<FluentTask<Integer, Void, Library>, Library> onGetLibraryComplete) {
		ExecuteGetLibrary(new FluentTask<Integer, Void, Library>() {
			@Override
			protected Library executeInBackground(Integer... params) {
				return GetLibrary(context, libraryId);
			}
		}, onGetLibraryComplete);
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
			return
				repositoryAccessHelper
					.mapSql("SELECT * FROM " + Library.tableName + " WHERE id = :id")
					.addParameter("id", libraryId)
					.fetchFirst(Library.class);
		} finally {
			repositoryAccessHelper.close();
		}
	}
	
	public static void GetLibraries(final Context context, ITwoParameterRunnable<FluentTask<Void, Void, List<Library>>, List<Library>> onGetLibrariesComplete) {
		final FluentTask<Void, Void, List<Library>> getLibrariesTask = new FluentTask<Void, Void, List<Library>>() {
			@Override
			protected List<Library> executeInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					return
						repositoryAccessHelper
							.mapSql("SELECT * FROM " + Library.tableName)
							.fetch(Library.class);
				} finally {
					repositoryAccessHelper.close();
				}
			}
		};

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
