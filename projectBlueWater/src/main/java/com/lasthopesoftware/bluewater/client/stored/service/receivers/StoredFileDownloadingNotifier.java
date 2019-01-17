package com.lasthopesoftware.bluewater.client.stored.service.receivers;

import android.content.Context;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.service.notifications.PostSyncNotification;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StoredFileDownloadingNotifier implements ReceiveStoredFileEvent {

	private static Map<Integer, CachedFilePropertiesProvider> filePropertiesProviderCache = new ConcurrentHashMap<>();

	private final CreateAndHold<String> downloadingStatusLabel = new AbstractSynchronousLazy<String>() {
		@Override
		protected String create() {
			return context.getString(R.string.downloading_status_label);
		}
	};

	private final IStoredFileAccess storedFileAccess;
	private final ILibraryProvider libraryProvider;
	private final BuildUrlProviders urlProviders;
	private final PostSyncNotification syncNotification;
	private final Context context;

	public StoredFileDownloadingNotifier(
		IStoredFileAccess storedFileAccess,
		ILibraryProvider libraryProvider,
		BuildUrlProviders urlProviders,
		PostSyncNotification syncNotification,
		Context context) {
		this.storedFileAccess = storedFileAccess;
		this.libraryProvider = libraryProvider;
		this.urlProviders = urlProviders;
		this.syncNotification = syncNotification;
		this.context = context;
	}

	@Override
	public Promise<Void> receive(int storedFileId) {
		return storedFileAccess.getStoredFile(storedFileId)
			.eventually(storedFile -> {
				final CachedFilePropertiesProvider cachedFilePropertiesProvider = filePropertiesProviderCache.get(storedFile.getLibraryId());
				if (cachedFilePropertiesProvider != null) {
					notifyOfFileDownload(cachedFilePropertiesProvider, storedFile);
					return Promise.empty();
				}

				return libraryProvider.getLibrary(storedFile.getLibraryId())
					.eventually(urlProviders::promiseBuiltUrlProvider)
					.then(new VoidResponse<>(urlProvider -> {
						final IConnectionProvider connectionProvider = new ConnectionProvider(urlProvider, OkHttpFactory.getInstance());
						final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
						final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertyCache,
							new FilePropertiesProvider(connectionProvider, filePropertyCache, ParsingScheduler.instance()));

						filePropertiesProviderCache.put(storedFile.getLibraryId(), filePropertiesProvider);
						notifyOfFileDownload(filePropertiesProvider, storedFile);
					}));
			});
	}

	@Override
	public Collection<String> acceptedEvents() {
		return Collections.singleton(StoredFileSynchronization.onFileDownloadingEvent);
	}

	private void notifyOfFileDownload(CachedFilePropertiesProvider filePropertiesProvider, StoredFile storedFile) {
		filePropertiesProvider.promiseFileProperties(new ServiceFile(storedFile.getServiceId()))
			.then(new VoidResponse<>(fileProperties -> syncNotification.notify(String.format(downloadingStatusLabel.getObject(), fileProperties.get(FilePropertiesProvider.NAME)))))
			.excuse(new VoidResponse<>(exception -> syncNotification.notify(String.format(downloadingStatusLabel.getObject(), context.getString(R.string.unknown_file)))));
	}
}
