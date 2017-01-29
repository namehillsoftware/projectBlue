package com.lasthopesoftware.bluewater.client.library.repository;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.repository.CloseableTransaction;
import com.lasthopesoftware.bluewater.repository.InsertBuilder;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.repository.UpdateBuilder;
import com.lasthopesoftware.bluewater.shared.DispatchedPromise.DispatchedPromise;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.promises.IPromise;
import com.vedsoft.fluent.FluentSpecifiedTask;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.lazyj.Lazy;
import com.vedsoft.objective.droid.ObjectiveDroid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LibrarySession {
	
	private static final Logger logger = LoggerFactory.getLogger(LibrarySession.class);
	public static final String libraryChosenEvent = MagicPropertyBuilder.buildMagicPropertyName(LibrarySession.class, "libraryChosenEvent");
	public static final String chosenLibraryInt = "chosen_library";

	private static final Lazy<String> libraryInsertSql
			= new Lazy<>(() ->
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
						.build());

	private static final Lazy<String> libraryUpdateSql
			= new Lazy<>(() ->
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
						.buildQuery());

	public static void saveLibrary(final Context context, final Library library, final OneParameterAction<Library> onSaveComplete) {
		final IPromise<Library> savedLibraryPromise = saveLibrary(context, library);

		if (onSaveComplete != null)
			savedLibraryPromise.then(VoidFunc.runningCarelessly(onSaveComplete));
	}

	@SuppressLint("NewApi")
	public static IPromise<Library> saveLibrary(final Context context, final Library library) {
		return new DispatchedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
					final boolean isLibraryExists = library.getId() > -1;

					final ObjectiveDroid objectiveDroid =
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
						objectiveDroid.addParameter("id", library.getId());

					final long result = objectiveDroid.execute();
					closeableTransaction.setTransactionSuccessful();

					if (!isLibraryExists)
						library.setId((int) result);

					logger.debug("Library saved.");
					return library;
				} catch (SQLException se) {
					logger.error("There was an error saving the library", se);
					return null;
				}
			}
		}, RepositoryAccessHelper.databaseExecutor);
	}

	public static void getActiveLibrary(final Context context, final OneParameterAction<Library> onGetLibraryComplete) {
		executeGetLibrary(new FluentSpecifiedTask<Integer, Void, Library>() {
			@Override
			protected Library executeInBackground(Integer... params) {
				return getActiveLibraryInternal(context);
			}
		}, onGetLibraryComplete);
	}

	public static IPromise<Library> getActiveLibrary(final Context context) {
		return new DispatchedPromise<>(onCanceled -> getActiveLibraryInternal(context), RepositoryAccessHelper.databaseExecutor);
	}

	public static IPromise<Library> getLibrary(final Context context, final int libraryId) {
		return new DispatchedPromise<>(onCancelled -> getLibraryInternal(context, libraryId), RepositoryAccessHelper.databaseExecutor);
	}

	public static void getLibrary(final Context context, final int libraryId, final OneParameterAction<Library> onGetLibraryComplete) {
		executeGetLibrary(new FluentSpecifiedTask<Integer, Void, Library>() {
			@Override
			protected Library executeInBackground(Integer... params) {
				return getLibraryInternal(context, libraryId);
			}
		}, onGetLibraryComplete);
	}

	private static void executeGetLibrary(FluentSpecifiedTask<Integer, Void, Library> getLibraryTask, final OneParameterAction<Library> onGetLibraryComplete) {
		getLibraryTask
				.onComplete(onGetLibraryComplete)
				.execute(RepositoryAccessHelper.databaseExecutor);
	}

	public static synchronized Library getActiveLibraryInternal(final Context context) {
		if ("Main".equals(Thread.currentThread().getName()))
			throw new IllegalStateException("This method must be called from a background thread.");

		final int chosenLibraryId = PreferenceManager.getDefaultSharedPreferences(context).getInt(chosenLibraryInt, -1);
		return chosenLibraryId >= 0 ? getLibraryInternal(context, chosenLibraryId) : null;
	}

	@SuppressLint("NewApi")
	private static synchronized Library getLibraryInternal(final Context context, int libraryId) {
		if (libraryId < 0) return null;

		try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
			return
					repositoryAccessHelper
							.mapSql("SELECT * FROM " + Library.tableName + " WHERE id = @id")
							.addParameter("id", libraryId)
							.fetchFirst(Library.class);
		}
	}

	@SuppressLint("NewApi")
	public static void getLibraries(final Context context, OneParameterAction<List<Library>> onGetLibrariesComplete) {
		new FluentSpecifiedTask<Void, Void, List<Library>>() {
			@Override
			protected List<Library> executeInBackground(Void... params) {
				try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
					return
							repositoryAccessHelper
									.mapSql("SELECT * FROM " + Library.tableName)
									.fetch(Library.class);
				}
			}
		}.onComplete(onGetLibrariesComplete).execute(RepositoryAccessHelper.databaseExecutor);
	}

	public synchronized static IPromise<Library> changeActiveLibrary(final Context context, final int libraryKey) {

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (libraryKey != sharedPreferences.getInt(chosenLibraryInt, -1))
            sharedPreferences.edit().putInt(chosenLibraryInt, libraryKey).apply();

		return
			getActiveLibrary(context).then(library -> {
				final Intent broadcastIntent = new Intent(libraryChosenEvent);
				broadcastIntent.putExtra(chosenLibraryInt, libraryKey);
				LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);

				return library;
			});
	}
}
