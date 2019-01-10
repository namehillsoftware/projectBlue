package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates;

import android.content.Context;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.LookupSyncDirectory;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.GetStoredFiles;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaFileIdProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.repository.CloseableTransaction;
import com.lasthopesoftware.bluewater.repository.InsertBuilder;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.repository.UpdateBuilder;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.lazyj.Lazy;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

import static com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess.storedFileAccessExecutor;

public class StoredFileUpdater implements UpdateStoredFiles {

	private static final Logger logger = LoggerFactory.getLogger(StoredFileUpdater.class);

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

	private static final Lazy<Pattern> reservedCharactersPattern = new Lazy<>(() -> Pattern.compile("[|?*<\":>+\\[\\]'/]"));

	private final Context context;
	private final MediaFileUriProvider mediaFileUriProvider;
	private final MediaFileIdProvider mediaFileIdProvider;
	private final GetStoredFiles storedFiles;
	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;
	private final LookupSyncDirectory lookupSyncDirectory;

	public StoredFileUpdater(
		Context context,
		MediaFileUriProvider mediaFileUriProvider,
		MediaFileIdProvider mediaFileIdProvider,
		GetStoredFiles storedFiles,
		CachedFilePropertiesProvider cachedFilePropertiesProvider,
		LookupSyncDirectory lookupSyncDirectory) {
		this.context = context;
		this.mediaFileUriProvider = mediaFileUriProvider;
		this.mediaFileIdProvider = mediaFileIdProvider;
		this.storedFiles = storedFiles;
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
		this.lookupSyncDirectory = lookupSyncDirectory;
	}

	@Override
	public Promise<StoredFile> promiseStoredFileUpdate(Library library, ServiceFile serviceFile) {
		return storedFiles.promiseStoredFile(library, serviceFile)
			.eventually(storedFile -> storedFile != null
				? new Promise<>(storedFile)
				: new QueuedPromise<>(() -> {
					try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
						logger.info("Stored serviceFile was not found for " + serviceFile.getKey() + ", creating serviceFile");
						createStoredFile(library, repositoryAccessHelper, serviceFile);
					}

					return null;
				}, storedFileAccessExecutor)
					.eventually(v -> storedFiles.promiseStoredFile(library, serviceFile)))
			.eventually(storedFile -> {
				if (storedFile.getPath() != null || !library.isUsingExistingFiles())
					return new Promise<>(storedFile);

				return mediaFileUriProvider
					.promiseFileUri(serviceFile)
					.eventually(localUri -> {
						if (localUri == null)
							return new Promise<>(storedFile);

						storedFile.setPath(localUri.getPath());
						storedFile.setIsDownloadComplete(true);
						storedFile.setIsOwner(false);
						return
							mediaFileIdProvider
								.getMediaId(serviceFile)
								.then(mediaId -> {
									storedFile.setStoredMediaId(mediaId);
									return storedFile;
								});
					});
			})
			.eventually(storedFile -> storedFile.getPath() != null
				? new Promise<>(storedFile)
				: cachedFilePropertiesProvider
					.promiseFileProperties(serviceFile)
					.eventually(fileProperties -> lookupSyncDirectory.promiseSyncDirectory(library)
						.then(syncDrive -> {
							String fullPath = syncDrive.getPath();

							String artist = fileProperties.get(FilePropertiesProvider.ALBUM_ARTIST);
							if (artist == null)
								artist = fileProperties.get(FilePropertiesProvider.ARTIST);

							if (artist != null)
								fullPath = FilenameUtils.concat(fullPath, replaceReservedCharsAndPath(artist.trim()));

							final String album = fileProperties.get(FilePropertiesProvider.ALBUM);
							if (album != null)
								fullPath = FilenameUtils.concat(fullPath, replaceReservedCharsAndPath(album.trim()));

							String fileName = fileProperties.get(FilePropertiesProvider.FILENAME);
							fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);

							final int extensionIndex = fileName.lastIndexOf('.');
							if (extensionIndex > -1)
								fileName = fileName.substring(0, extensionIndex + 1) + "mp3";

							fullPath = FilenameUtils.concat(fullPath, fileName).trim();

							storedFile.setPath(fullPath);

							return storedFile;
						})))
			.eventually(storedFile -> new QueuedPromise<>(() -> {
				try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
					updateStoredFile(repositoryAccessHelper, storedFile);
					return storedFile;
				}
			}, storedFileAccessExecutor));
	}

	private static String replaceReservedCharsAndPath(String path) {
		return reservedCharactersPattern.getObject().matcher(path).replaceAll("_");
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
}
