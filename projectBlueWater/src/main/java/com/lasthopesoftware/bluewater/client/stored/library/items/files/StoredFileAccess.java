package com.lasthopesoftware.bluewater.client.stored.library.items.files;

import android.content.Context;
import android.database.SQLException;

import androidx.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.GetAllStoredFilesInLibrary;
import com.lasthopesoftware.bluewater.repository.CloseableTransaction;
import com.lasthopesoftware.bluewater.repository.InsertBuilder;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.repository.UpdateBuilder;
import com.lasthopesoftware.resources.executors.CachedSingleThreadExecutor;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import kotlin.Unit;

public final class StoredFileAccess implements IStoredFileAccess {

	public static Executor storedFileAccessExecutor() {
		return storedFileAccessExecutor.getObject();
	}

	private static final CreateAndHold<Executor> storedFileAccessExecutor = new Lazy<>(CachedSingleThreadExecutor::new);

	private static final Logger logger = LoggerFactory.getLogger(StoredFileAccess.class);

	private final Context context;
	private final GetAllStoredFilesInLibrary getAllStoredFilesInLibrary;

	private static final String selectFromStoredFiles = "SELECT * FROM " + StoredFileEntityInformation.tableName;

	private static final Lazy<String> insertSql
			= new Lazy<>(() ->
				InsertBuilder
						.fromTable(StoredFileEntityInformation.tableName)
						.addColumn(StoredFileEntityInformation.serviceIdColumnName)
						.addColumn(StoredFileEntityInformation.libraryIdColumnName)
						.addColumn(StoredFileEntityInformation.isOwnerColumnName)
						.build());

	private static final Lazy<String> updateSql =
			new Lazy<>(() ->
					UpdateBuilder
							.fromTable(StoredFileEntityInformation.tableName)
							.addSetter(StoredFileEntityInformation.serviceIdColumnName)
							.addSetter(StoredFileEntityInformation.storedMediaIdColumnName)
							.addSetter(StoredFileEntityInformation.pathColumnName)
							.addSetter(StoredFileEntityInformation.isOwnerColumnName)
							.addSetter(StoredFileEntityInformation.isDownloadCompleteColumnName)
							.setFilter("WHERE id = @id")
							.buildQuery());

	public StoredFileAccess(
		Context context,
		GetAllStoredFilesInLibrary getAllStoredFilesInLibrary) {

		this.context = context;
		this.getAllStoredFilesInLibrary = getAllStoredFilesInLibrary;
	}

	@Override
	public Promise<StoredFile> getStoredFile(final int storedFileId) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return getStoredFile(repositoryAccessHelper, storedFileId);
			}
		}, storedFileAccessExecutor.getObject());
	}

	@Override
	public Promise<StoredFile> getStoredFile(Library library, final ServiceFile serviceFile) {
		return getStoredFileTask(library, serviceFile);
	}

	private Promise<StoredFile> getStoredFileTask(Library library, final ServiceFile serviceFile) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return getStoredFile(library, repositoryAccessHelper, serviceFile);
			}
		}, storedFileAccessExecutor.getObject());
	}

	@Override
	public Promise<List<StoredFile>> getDownloadingStoredFiles() {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return repositoryAccessHelper
					.mapSql(
						selectFromStoredFiles + " WHERE " + StoredFileEntityInformation.isDownloadCompleteColumnName + " = @" + StoredFileEntityInformation.isDownloadCompleteColumnName)
					.addParameter(StoredFileEntityInformation.isDownloadCompleteColumnName, false)
					.fetch(StoredFile.class);
			}
		}, storedFileAccessExecutor.getObject());
	}

	@Override
	public Promise<StoredFile> markStoredFileAsDownloaded(final StoredFile storedFile) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {

					repositoryAccessHelper
							.mapSql(
								" UPDATE " + StoredFileEntityInformation.tableName +
								" SET " + StoredFileEntityInformation.isDownloadCompleteColumnName + " = 1" +
								" WHERE id = @id")
							.addParameter("id", storedFile.getId())
							.execute();

					closeableTransaction.setTransactionSuccessful();
				}
			}

			storedFile.setIsDownloadComplete(true);
			return storedFile;
		}, storedFileAccessExecutor.getObject());
	}

	@Override
	public Promise<Void> addMediaFile(Library library, final ServiceFile serviceFile, final int mediaFileId, final String filePath) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				StoredFile storedFile = getStoredFile(library, repositoryAccessHelper, serviceFile);

				if (storedFile == null) {
					createStoredFile(library, repositoryAccessHelper, serviceFile);
					storedFile = getStoredFile(library, repositoryAccessHelper, serviceFile)
						.setIsOwner(false)
						.setIsDownloadComplete(true)
						.setPath(filePath);
				}

				storedFile.setStoredMediaId(mediaFileId);
				updateStoredFile(repositoryAccessHelper, storedFile);

				return null;
			}
		}, storedFileAccessExecutor.getObject());
	}

	@Override
	public Promise<Unit> pruneStoredFiles(LibraryId libraryId, @NonNull final Set<ServiceFile> serviceFilesToKeep) {
		return getAllStoredFilesInLibrary.promiseAllStoredFiles(libraryId)
			.eventually(new PruneFilesTask(this, serviceFilesToKeep));
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

	private void createStoredFile(Library library, RepositoryAccessHelper repositoryAccessHelper, ServiceFile serviceFile) {
		try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
			repositoryAccessHelper
					.mapSql(insertSql.getObject())
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, serviceFile.getKey())
					.addParameter(StoredFileEntityInformation.libraryIdColumnName, library.getId())
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, true)
					.execute();

			closeableTransaction.setTransactionSuccessful();
		}
	}

	private static void updateStoredFile(RepositoryAccessHelper repositoryAccessHelper, StoredFile storedFile) {
		try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
			repositoryAccessHelper
					.mapSql(updateSql.getObject())
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, storedFile.getServiceId())
					.addParameter(StoredFileEntityInformation.storedMediaIdColumnName, storedFile.getStoredMediaId())
					.addParameter(StoredFileEntityInformation.pathColumnName, storedFile.getPath())
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, storedFile.isOwner())
					.addParameter(StoredFileEntityInformation.isDownloadCompleteColumnName, storedFile.isDownloadComplete())
					.addParameter("id", storedFile.getId())
					.execute();

			closeableTransaction.setTransactionSuccessful();
		}
	}

	void deleteStoredFile(final StoredFile storedFile) {
		storedFileAccessExecutor.getObject().execute(() -> {
			try (final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				try (final CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {

					repositoryAccessHelper
						.mapSql("DELETE FROM " + StoredFileEntityInformation.tableName + " WHERE id = @id")
						.addParameter("id", storedFile.getId())
						.execute();

					closeableTransaction.setTransactionSuccessful();
				} catch (SQLException e) {
					logger.error("There was an error deleting serviceFile " + storedFile.getId(), e);
				}
			}
		});
	}
}
