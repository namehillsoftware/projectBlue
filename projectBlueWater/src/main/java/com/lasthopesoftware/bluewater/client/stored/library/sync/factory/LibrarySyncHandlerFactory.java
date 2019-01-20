package com.lasthopesoftware.bluewater.client.stored.library.sync.factory;

import android.content.Context;
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.FileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.io.IFileStreamWriter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.items.playlists.PlaylistItemFinder;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.views.access.LibraryViewsByConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.views.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.stored.library.items.conversion.StoredPlaylistItemsConverter;
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
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;
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

	public LibrarySyncHandlerFactory(
		IStoredFileAccess storedFileAccess,
		Context context,
		IStorageReadPermissionArbitratorForOs storageReadPermissionsPossible,
		LookupSyncDirectory syncDirectory,
		IStoredFileSystemFileProducer storedFileSystemFileProducer,
		IServiceFileUriQueryParamsProvider serviceFileUriQueryParamsProvider,
		IFileReadPossibleArbitrator fileReadPossibleArbitrator,
		IFileWritePossibleArbitrator fileWritePossibleArbitrator,
		IFileStreamWriter fileStreamWriter) {
		this.storedFileAccess = storedFileAccess;
		this.context = context;
		this.storageReadPermissionsPossible = storageReadPermissionsPossible;
		this.syncDirectory = syncDirectory;
		this.storedFileSystemFileProducer = storedFileSystemFileProducer;
		this.serviceFileUriQueryParamsProvider = serviceFileUriQueryParamsProvider;
		this.fileReadPossibleArbitrator = fileReadPossibleArbitrator;
		this.fileWritePossibleArbitrator = fileWritePossibleArbitrator;
		this.fileStreamWriter = fileStreamWriter;
	}

	@Override
	public LibrarySyncHandler getNewSyncHandler(IUrlProvider urlProvider, Library library) {
		final ConnectionProvider connectionProvider = new ConnectionProvider(urlProvider, OkHttpFactory.getInstance());

		final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
		final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(
			connectionProvider,
			filePropertyCache,
			new FilePropertiesProvider(
				connectionProvider,
				filePropertyCache,
				ParsingScheduler.instance()));

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
			cachedFilePropertiesProvider,
			syncDirectory);

		final StoredItemAccess storedItemAccess = new StoredItemAccess(
			context,
			library);

		return new LibrarySyncHandler(
			library,
			new StoredItemServiceFileCollector(
				storedItemAccess,
				new StoredPlaylistItemsConverter(
					new PlaylistItemFinder(
						new LibraryViewsProvider(connectionProvider, new LibraryViewsByConnectionProvider()),
						new ItemProvider(connectionProvider)),
					storedItemAccess),
				new FileProvider(new FileStringListProvider(connectionProvider))),
			storedFileAccess,
			storedFileUpdater,
			new StoredFileJobProcessor(
				storedFileSystemFileProducer,
				connectionProvider,
				storedFileAccess,
				new StoredFileDownloader(serviceFileUriQueryParamsProvider, connectionProvider),
				serviceFileUriQueryParamsProvider,
				fileReadPossibleArbitrator,
				fileWritePossibleArbitrator,
				fileStreamWriter)
		);
	}
}
