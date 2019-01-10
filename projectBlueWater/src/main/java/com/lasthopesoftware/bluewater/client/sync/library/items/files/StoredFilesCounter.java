package com.lasthopesoftware.bluewater.client.sync.library.items.files;

import android.content.Context;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.sync.library.items.files.repository.StoredFileEntityInformation;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;

public class StoredFilesCounter implements CountStoredFiles {
	private final Context context;

	public StoredFilesCounter(Context context) {
		this.context = context;
	}

	@Override
	public Promise<Long> promiseStoredFilesCount(Library library) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return repositoryAccessHelper
					.mapSql("SELECT COUNT(*) FROM " + StoredFileEntityInformation.tableName + " WHERE " + StoredFileEntityInformation.libraryIdColumnName + " + @" + StoredFileEntityInformation.libraryIdColumnName)
					.addParameter(StoredFileEntityInformation.libraryIdColumnName, library.getId())
					.execute();
			}
		}, RepositoryAccessHelper.databaseExecutor);
	}
}
