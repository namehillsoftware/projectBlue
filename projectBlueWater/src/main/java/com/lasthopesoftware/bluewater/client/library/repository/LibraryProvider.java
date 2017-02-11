package com.lasthopesoftware.bluewater.client.library.repository;

import android.annotation.SuppressLint;
import android.content.Context;

import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.shared.DispatchedPromise.DispatchedPromise;
import com.lasthopesoftware.promises.IPromise;
import com.vedsoft.futures.callables.CarelessFunction;

/**
 * Created by david on 2/11/17.
 */

public class LibraryProvider {
	private final Context context;

	public LibraryProvider(Context context) {
		this.context = context;
	}

	public IPromise<Library> get(int libraryId) {
		return new DispatchedPromise<>(new GetLibraryTask(context, libraryId), RepositoryAccessHelper.databaseExecutor);
	}

	private static class GetLibraryTask implements CarelessFunction<Library> {

		private int libraryId;
		private Context context;

		private GetLibraryTask(Context context, int libraryId) {
			this.libraryId = libraryId;
			this.context = context;
		}

		@SuppressLint("NewApi")
		@Override
		public Library result() throws Exception {
			if (libraryId < 0) return null;

			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return
					repositoryAccessHelper
						.mapSql("SELECT * FROM " + Library.tableName + " WHERE id = @id")
						.addParameter("id", libraryId)
						.fetchFirst(Library.class);
			}
		}
	}
}
