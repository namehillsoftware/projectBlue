package com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;

import static com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess.storedFileAccessExecutor;

public class StoredFileQuery implements GetStoredFiles {

	private final Context context;

	public StoredFileQuery(Context context) {
		this.context = context;
	}

	@Override
	public Promise<StoredFile> promiseStoredFile(LibraryId libraryId, ServiceFile serviceFile) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return getStoredFile(libraryId, repositoryAccessHelper, serviceFile);
			}
		}, storedFileAccessExecutor());
	}

	private StoredFile getStoredFile(LibraryId libraryId, RepositoryAccessHelper helper, ServiceFile serviceFile) {
		return
			helper
				.mapSql(
					" SELECT * " +
						" FROM " + StoredFileEntityInformation.tableName + " " +
						" WHERE " + StoredFileEntityInformation.serviceIdColumnName + " = @" + StoredFileEntityInformation.serviceIdColumnName +
						" AND " + StoredFileEntityInformation.libraryIdColumnName + " = @" + StoredFileEntityInformation.libraryIdColumnName)
				.addParameter(StoredFileEntityInformation.serviceIdColumnName, serviceFile.getKey())
				.addParameter(StoredFileEntityInformation.libraryIdColumnName, libraryId.getId())
				.fetchFirst(StoredFile.class);
	}

	private StoredFile getStoredFile(RepositoryAccessHelper helper, int storedFileId) {
		return
			helper
				.mapSql("SELECT * FROM " + StoredFileEntityInformation.tableName + " WHERE id = @id")
				.addParameter("id", storedFileId)
				.fetchFirst(StoredFile.class);
	}
}
