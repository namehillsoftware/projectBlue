package com.lasthopesoftware.bluewater.client.library.repository;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.PreferenceManager;

import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.promises.IPromise;
import com.vedsoft.fluent.FluentSpecifiedTask;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.List;

public class LibrarySession {

	public static final String libraryChosenEvent = MagicPropertyBuilder.buildMagicPropertyName(LibrarySession.class, "libraryChosenEvent");
	public static final String chosenLibraryInt = "chosen_library";

	public static void saveLibrary(final Context context, final Library library, final OneParameterAction<Library> onSaveComplete) {
		final IPromise<Library> savedLibraryPromise = saveLibrary(context, library);

		if (onSaveComplete != null)
			savedLibraryPromise.then(VoidFunc.runningCarelessly(onSaveComplete));
	}

	@SuppressLint("NewApi")
	public static IPromise<Library> saveLibrary(final Context context, final Library library) {
		return new LibraryRepository(context).saveLibrary(library);
	}

	public static void getActiveLibrary(final Context context, final OneParameterAction<Library> onGetLibraryComplete) {
		executeGetLibrary(new FluentSpecifiedTask<Integer, Void, Library>() {
			@Override
			protected Library executeInBackground(Integer... params) {
				return getActiveLibraryInternal(context);
			}
		}, onGetLibraryComplete);
	}

	public static IPromise<Library> getLibrary(final Context context, final int libraryId) {
		return new LibraryRepository(context).getLibrary(libraryId);
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
	private static Library getLibraryInternal(final Context context, int libraryId) {
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

}
