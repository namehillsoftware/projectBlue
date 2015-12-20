package com.lasthopesoftware.bluewater.servers.library.repository;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.repository.InsertBuilder;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.repository.UpdateBuilder;
import com.lasthopesoftware.bluewater.shared.SpecialValueHelpers;
import com.lasthopesoftware.sql.SqlMapper;
import com.lasthopesoftware.threading.FluentTask;
import com.lasthopesoftware.threading.Lazy;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;

public class LibrarySession {
	
	private static final Logger logger = LoggerFactory.getLogger(LibrarySession.class);
	public static final String libraryChosenEvent = SpecialValueHelpers.buildMagicPropertyName(LibrarySession.class, "libraryChosenEvent");
	public static final String chosenLibraryInt = "chosen_library";

	private static final Lazy<String> libraryInsertSql = new Lazy<>(new Callable<String>() {
		@Override
		public String call() throws Exception {
			return
					InsertBuilder
							.fromTable(Library.tableName)
							.addColumn(Library.accessCodeColumn)
							.addColumn(Library.authKeyColumn)
							.addColumn(Library.isLocalOnlyColumn)
							.addColumn(Library.libraryNameColumn)
							.addColumn(Library.isRepeatingColumn)
							.addColumn(Library.customSyncedFilesPathColumn)
							.addColumn(Library.isSyncLocalConnectionsOnlyColumn)
							.addColumn(Library.isUsingExistingFilesColumn)
							.addColumn(Library.nowPlayingIdColumn)
							.addColumn(Library.nowPlayingProgressColumn)
							.addColumn(Library.savedTracksStringColumn)
							.addColumn(Library.selectedViewColumn)
							.addColumn(Library.selectedViewTypeColumn)
							.addColumn(Library.syncedFileLocationColumn)
							.build();
		}
	});

	private static final Lazy<String> libraryUpdateSql = new Lazy<>(new Callable<String>() {
		@Override
		public String call() throws Exception {
			return
					UpdateBuilder
							.fromTable(Library.tableName)
							.addSetter(Library.accessCodeColumn)
							.addSetter(Library.authKeyColumn)
							.addSetter(Library.isLocalOnlyColumn)
							.addSetter(Library.libraryNameColumn)
							.addSetter(Library.isRepeatingColumn)
							.addSetter(Library.customSyncedFilesPathColumn)
							.addSetter(Library.isSyncLocalConnectionsOnlyColumn)
							.addSetter(Library.isUsingExistingFilesColumn)
							.addSetter(Library.nowPlayingIdColumn)
							.addSetter(Library.nowPlayingProgressColumn)
							.addSetter(Library.savedTracksStringColumn)
							.addSetter(Library.selectedViewColumn)
							.addSetter(Library.selectedViewTypeColumn)
							.addSetter(Library.syncedFileLocationColumn)
							.setFilter("WHERE id = @id")
							.buildQuery();
		}
	});

	public static void SaveLibrary(final Context context, final Library library) {
		SaveLibrary(context, library, null);
	}

	public static void SaveLibrary(final Context context, final Library library, final TwoParameterRunnable<FluentTask<Void, Void, Library>, Library> onSaveComplete) {

		final FluentTask<Void, Void, Library> writeToDatabaseTask = new FluentTask<Void, Void, Library>() {

			@Override
			protected Library executeInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					final boolean isLibraryExists = library.getId() > -1;

					final SqlMapper sqlMapper =
						repositoryAccessHelper
								.mapSql(isLibraryExists ? libraryUpdateSql.getObject() : libraryInsertSql.getObject())
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
								.addParameter(Library.syncedFileLocationColumn, library.getSyncedFileLocation());

					if (isLibraryExists)
						sqlMapper.addParameter("id", library.getId());

					final long result = sqlMapper.execute();

					if (!isLibraryExists)
						library.setId((int)result);

					logger.debug("Library saved.");
					return library;
				} catch (SQLException se) {
					logger.error("There was an error saving the library", se);
					return null;
				} finally {
					repositoryAccessHelper.close();
				}
			}
		};

		if (onSaveComplete != null)
			writeToDatabaseTask.onComplete(onSaveComplete);

		writeToDatabaseTask.execute(RepositoryAccessHelper.databaseExecutor);
	}

	public static void GetActiveLibrary(final Context context, final TwoParameterRunnable<FluentTask<Integer, Void, Library>, Library> onGetLibraryComplete) {
		ExecuteGetLibrary(new FluentTask<Integer, Void, Library>() {
			@Override
			protected Library executeInBackground(Integer... params) {
				return GetActiveLibrary(context);
			}
		}, onGetLibraryComplete);
	}

	public static void GetLibrary(final Context context, final int libraryId, final TwoParameterRunnable<FluentTask<Integer, Void, Library>, Library> onGetLibraryComplete) {
		ExecuteGetLibrary(new FluentTask<Integer, Void, Library>() {
			@Override
			protected Library executeInBackground(Integer... params) {
				return GetLibrary(context, libraryId);
			}
		}, onGetLibraryComplete);
	}

	private static void ExecuteGetLibrary(FluentTask<Integer, Void, Library> getLibraryTask, final TwoParameterRunnable<FluentTask<Integer, Void, Library>, Library> onGetLibraryComplete) {

		getLibraryTask.onComplete(new TwoParameterRunnable<FluentTask<Integer, Void, Library>, Library>() {

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
					.mapSql("SELECT * FROM " + Library.tableName + " WHERE id = @id")
					.addParameter("id", libraryId)
					.fetchFirst(Library.class);
		} finally {
			repositoryAccessHelper.close();
		}
	}
	
	public static void GetLibraries(final Context context, TwoParameterRunnable<FluentTask<Void, Void, List<Library>>, List<Library>> onGetLibrariesComplete) {
		new FluentTask<Void, Void, List<Library>>() {
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
		}.onComplete(onGetLibrariesComplete).execute(RepositoryAccessHelper.databaseExecutor);
	}

	public synchronized static void ChooseLibrary(final Context context, final int libraryKey, final TwoParameterRunnable<FluentTask<Integer, Void, Library>, Library> onLibraryChangeComplete) {

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (libraryKey != sharedPreferences.getInt(chosenLibraryInt, -1)) {
            sharedPreferences.edit().putInt(chosenLibraryInt, libraryKey).apply();
		}
		
		GetActiveLibrary(context, new TwoParameterRunnable<FluentTask<Integer, Void, Library>, Library>() {
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
