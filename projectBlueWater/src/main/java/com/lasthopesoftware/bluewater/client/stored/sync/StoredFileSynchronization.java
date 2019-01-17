package com.lasthopesoftware.bluewater.client.stored.sync;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncHandler;
import com.lasthopesoftware.bluewater.client.stored.library.sync.factory.ProduceLibrarySyncHandlers;
import com.lasthopesoftware.bluewater.client.stored.service.notifications.PostSyncNotification;
import com.lasthopesoftware.bluewater.client.stored.worker.SyncSchedulingWorker;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.observables.ObservedPromise;
import com.lasthopesoftware.bluewater.shared.observables.StreamedPromise;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;
import io.reactivex.Completable;
import io.reactivex.exceptions.CompositeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoredFileSynchronization implements SynchronizeStoredFiles {

	private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(SyncSchedulingWorker.class);

	public static final String onSyncStartEvent = magicPropertyBuilder.buildProperty("onSyncStartEvent");
	public static final String onSyncStopEvent = magicPropertyBuilder.buildProperty("onSyncStopEvent");
	public static final String onFileQueuedEvent = magicPropertyBuilder.buildProperty("onFileQueuedEvent");
	public static final String onFileDownloadingEvent = magicPropertyBuilder.buildProperty("onFileDownloadingEvent");
	public static final String onFileDownloadedEvent = magicPropertyBuilder.buildProperty("onFileDownloadedEvent");
	public static final String onFileWriteErrorEvent = magicPropertyBuilder.buildProperty("onFileWriteErrorEvent");
	public static final String onFileReadErrorEvent = magicPropertyBuilder.buildProperty("onFileReadErrorEvent");
	public static final String storedFileEventKey = magicPropertyBuilder.buildProperty("storedFileEventKey");

	private static final Logger logger = LoggerFactory.getLogger(StoredFileSynchronization.class);
	private final ILibraryProvider libraryProvider;
	private final LocalBroadcastManager localBroadcastManager;
	private final BuildUrlProviders urlProviders;
	private final ProduceLibrarySyncHandlers librarySyncHandlersProduction;
	private final PostSyncNotification syncNotifications;

	public StoredFileSynchronization(
		ILibraryProvider libraryProvider,
		LocalBroadcastManager localBroadcastManager,
		BuildUrlProviders urlProviders,
		ProduceLibrarySyncHandlers librarySyncHandlersProduction,
		PostSyncNotification syncNotifications) {
		this.libraryProvider = libraryProvider;
		this.localBroadcastManager = localBroadcastManager;
		this.urlProviders = urlProviders;
		this.librarySyncHandlersProduction = librarySyncHandlersProduction;
		this.syncNotifications = syncNotifications;
	}

	@Override
	public Completable streamFileSynchronization() {
		logger.info("Starting sync.");

		syncNotifications.notify(null);
		localBroadcastManager.sendBroadcast(new Intent(onSyncStartEvent));

		return StreamedPromise.stream(libraryProvider.getAllLibraries())
			.map(library -> {
				if (library.isSyncLocalConnectionsOnly()) library.setLocalOnly(true);

				return ObservedPromise.observe(urlProviders.promiseBuiltUrlProvider(library)
					.then(urlProvider -> librarySyncHandlersProduction.getNewSyncHandler(urlProvider, library)));
			})
			.flatMap(o -> o.flatMap(LibrarySyncHandler::observeLibrarySync, true))
			.flatMapCompletable(storedFileJobStatus -> {
				switch (storedFileJobStatus.storedFileJobState) {
					case Queued:
						sendStoredFileBroadcast(onFileQueuedEvent, storedFileJobStatus.storedFile);
						return Completable.complete();
					case Downloading:
						sendStoredFileBroadcast(onFileDownloadingEvent, storedFileJobStatus.storedFile);
						return Completable.complete();
					case Downloaded:
						sendStoredFileBroadcast(onFileDownloadedEvent, storedFileJobStatus.storedFile);
						return Completable.complete();
				}

				return Completable.complete();
			}, true)
			.onErrorComplete(this::handleError)
			.doOnComplete(() -> localBroadcastManager.sendBroadcast(new Intent(onSyncStopEvent)));
	}

	private boolean handleError(Throwable e) {
		if (e instanceof CompositeException) {
			final CompositeException compositeException = (CompositeException)e;
			return Stream.of(compositeException.getExceptions()).allMatch(this::handleError);
		}

		if (e instanceof StoredFileWriteException) {
			sendStoredFileBroadcast(onFileWriteErrorEvent, ((StoredFileWriteException)e).getStoredFile());
			return true;
		}

		if (e instanceof StoredFileReadException) {
			sendStoredFileBroadcast(onFileReadErrorEvent, ((StoredFileReadException)e).getStoredFile());
			return true;
		}

		return e instanceof StorageCreatePathException || e instanceof StoredFileJobException;
	}

	private void sendStoredFileBroadcast(@NonNull String action, @NonNull StoredFile storedFile) {
		final Intent storedFileBroadcastIntent = new Intent(action);
		storedFileBroadcastIntent.putExtra(storedFileEventKey, storedFile.getId());
		localBroadcastManager.sendBroadcast(storedFileBroadcastIntent);
	}
}
