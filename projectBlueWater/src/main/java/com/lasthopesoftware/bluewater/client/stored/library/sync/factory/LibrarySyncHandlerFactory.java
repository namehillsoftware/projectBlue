package com.lasthopesoftware.bluewater.client.stored.library.sync.factory;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.LibraryFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.LibraryFileStringListProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.io.IFileStreamWriter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaFileIdProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaQueryCursorProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater;
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncHandler;
import com.lasthopesoftware.bluewater.client.stored.library.sync.LookupSyncDirectory;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;

public class LibrarySyncHandlerFactory implements ProduceLibrarySyncHandlers {

	private final IStoredFileAccess storedFileAccess;
	private final Context context;
	private final IStorageReadPermissionArbitratorForOs storageReadPermissionsPossible;
	private final LookupSyncDirectory syncDirectory;
	private final IStoredFileSystemFileProducer storedFileSystemFileProducer;
	private final IServiceFileUriQueryParamsProvider serviceFileUriQueryParamsProvider;
	private final IFileReadPossibleArbitrator fileReadPossibleArbitrator;
	private final IFileWritePossibleArbitrator fileWritePossibleArbitrator;
	private final IFileStreamWriter fileStreamWriter;
	private final ILibraryProvider libraryProvider;
	private final ProvideLibraryConnections libraryConnections;

	public LibrarySyncHandlerFactory(
		IStoredFileAccess storedFileAccess,
		Context context,
		IStorageReadPermissionArbitratorForOs storageReadPermissionsPossible,
		LookupSyncDirectory syncDirectory,
		IStoredFileSystemFileProducer storedFileSystemFileProducer,
		IServiceFileUriQueryParamsProvider serviceFileUriQueryParamsProvider,
		IFileReadPossibleArbitrator fileReadPossibleArbitrator,
		IFileWritePossibleArbitrator fileWritePossibleArbitrator,
		IFileStreamWriter fileStreamWriter,
		ILibraryProvider libraryProvider,
		ProvideLibraryConnections libraryConnections) {
		this.storedFileAccess = storedFileAccess;
		this.context = context;
		this.storageReadPermissionsPossible = storageReadPermissionsPossible;
		this.syncDirectory = syncDirectory;
		this.storedFileSystemFileProducer = storedFileSystemFileProducer;
		this.serviceFileUriQueryParamsProvider = serviceFileUriQueryParamsProvider;
		this.fileReadPossibleArbitrator = fileReadPossibleArbitrator;
		this.fileWritePossibleArbitrator = fileWritePossibleArbitrator;
		this.fileStreamWriter = fileStreamWriter;
		this.libraryProvider = libraryProvider;
		this.libraryConnections = libraryConnections;
	}

	@Override
	public LibrarySyncHandler getNewSyncHandler(IUrlProvider urlProvider, Library library) {

		final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
		final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(
			libraryConnections,
			filePropertyCache,
			new FilePropertiesProvider(
				libraryConnections,
				filePropertyCache));

		final MediaQueryCursorProvider cursorProvider = new MediaQueryCursorProvider(
			context,
			cachedFilePropertiesProvider);

		final StoredFileUpdater storedFileUpdater = new StoredFileUpdater(
			context,
			new MediaFileUriProvider(
				context,
				cursorProvider,
				storageReadPermissionsPossible,
				library,
				true),
			new MediaFileIdProvider(
				cursorProvider,
				storageReadPermissionsPossible),
			new StoredFileQuery(context),
			libraryProvider,
			cachedFilePropertiesProvider,
			syncDirectory);

		final StoredItemAccess storedItemAccess = new StoredItemAccess(context);

		return new LibrarySyncHandler(
			new StoredItemServiceFileCollector(
				storedItemAccess,
				new LibraryFileProvider(new LibraryFileStringListProvider(libraryConnections)),
				FileListParameters.getInstance()),
			storedFileAccess,
			storedFileUpdater,
			new StoredFileJobProcessor(
				storedFileSystemFileProducer,
				storedFileAccess,
				new StoredFileDownloader(serviceFileUriQueryParamsProvider, libraryConnections),
				fileReadPossibleArbitrator,
				fileWritePossibleArbitrator,
				fileStreamWriter));
	}
}
