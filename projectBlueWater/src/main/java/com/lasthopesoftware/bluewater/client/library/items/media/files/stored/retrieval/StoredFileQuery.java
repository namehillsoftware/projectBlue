package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.retrieval;

import android.content.Context;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFileEntityInformation;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;

import static com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFileAccess.storedFileAccessExecutor;

public class StoredFileQuery implements GetStoredFiles {

	private final Context context;

	public StoredFileQuery(Context context) {
		this.context = context;
	}

	@Override
	public Promise<StoredFile> promiseStoredFile(Library library, ServiceFile serviceFile) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return getStoredFile(library, repositoryAccessHelper, serviceFile);
			}
		}, storedFileAccessExecutor);
	}

	@Override
	public Promise<StoredFile> promiseStoredFile(int storedFileId) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return getStoredFile(repositoryAccessHelper, storedFileId);
			}
		}, storedFileAccessExecutor);
	}

	private StoredFile getStoredFile(Library library, RepositoryAccessHelper helper, ServiceFile serviceFile) {
		return
			helper
				.mapSql(
					" SELECT * " +
						" FROM " + StoredFileEntityInformation.tableName + " " +
						" WHERE " + StoredFileEntityInformation.serviceIdColumnName + " = @" + StoredFileEntityInformation.serviceIdColumnName +
						" AND " + StoredFileEntityInformation.libraryIdColumnName + " = @" + StoredFileEntityInformation.libraryIdColumnName)
				.addParameter(StoredFileEntityInformation.serviceIdColumnName, serviceFile.getKey())
				.addParameter(StoredFileEntityInformation.libraryIdColumnName, library.getId())
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
