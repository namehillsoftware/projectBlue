package com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval;

import android.content.Context;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;

import java.util.Collection;

public class StoredFilesCollection implements GetAllStoredFilesInLibrary {
	private final Context context;

	public StoredFilesCollection(Context context) {
		this.context = context;
	}

	@Override
	public Promise<Collection<StoredFile>> promiseAllStoredFiles(Library library) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return repositoryAccessHelper
					.mapSql("SELECT * FROM " + StoredFileEntityInformation.tableName + " WHERE " + StoredFileEntityInformation.libraryIdColumnName + " = @" + StoredFileEntityInformation.libraryIdColumnName)
					.addParameter(StoredFileEntityInformation.libraryIdColumnName, library.getId())
					.fetch(StoredFile.class);
			}
		}, RepositoryAccessHelper.databaseExecutor);
	}
}
