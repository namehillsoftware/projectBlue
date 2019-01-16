package com.lasthopesoftware.bluewater.client.stored.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders;
import com.lasthopesoftware.bluewater.client.library.BrowseLibraryActivity;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.broadcasts.IScanMediaFileBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.broadcasts.ScanMediaFileBroadcaster;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.read.IStorageReadPermissionsRequestedBroadcast;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.read.StorageReadPermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write.IStorageWritePermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write.StorageWritePermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncHandler;
import com.lasthopesoftware.bluewater.client.stored.library.sync.factory.ProduceLibrarySyncHandlers;
import com.lasthopesoftware.bluewater.client.stored.worker.SyncSchedulingWorker;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.observables.ObservedPromise;
import com.lasthopesoftware.bluewater.shared.observables.StreamedPromise;
import com.lasthopesoftware.resources.notifications.notificationchannel.ChannelConfiguration;
import com.lasthopesoftware.resources.notifications.notificationchannel.NotificationChannelActivator;
import com.lasthopesoftware.resources.notifications.notificationchannel.SharedChannelProperties;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;
import com.lasthopesoftware.storage.write.permissions.ExternalStorageWritePermissionsArbitratorForOs;
import com.lasthopesoftware.storage.write.permissions.IStorageWritePermissionArbitratorForOs;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import io.reactivex.Completable;
import io.reactivex.exceptions.CompositeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static android.content.Context.NOTIFICATION_SERVICE;

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

	private static final int notificationId = 23;

	private static final Logger logger = LoggerFactory.getLogger(StoredFileSynchronization.class);

	private final CreateAndHold<IStorageReadPermissionsRequestedBroadcast> storageReadPermissionsRequestedBroadcast = new AbstractSynchronousLazy<IStorageReadPermissionsRequestedBroadcast>() {
		@Override
		protected IStorageReadPermissionsRequestedBroadcast create() {
			return new StorageReadPermissionsRequestedBroadcaster(localBroadcastManager);
		}
	};

	private final CreateAndHold<IStorageWritePermissionsRequestedBroadcaster> storageWritePermissionsRequestedBroadcast = new AbstractSynchronousLazy<IStorageWritePermissionsRequestedBroadcaster>() {
		@Override
		protected IStorageWritePermissionsRequestedBroadcaster create() {
			return new StorageWritePermissionsRequestedBroadcaster(localBroadcastManager);
		}
	};

	private final CreateAndHold<IStorageReadPermissionArbitratorForOs> storageReadPermissionArbitratorForOsLazy = new AbstractSynchronousLazy<IStorageReadPermissionArbitratorForOs>() {
		@Override
		protected IStorageReadPermissionArbitratorForOs create() {
			return new ExternalStorageReadPermissionsArbitratorForOs(context);
		}
	};

	private final CreateAndHold<IStorageWritePermissionArbitratorForOs> storageWritePermissionArbitratorForOsLazy = new AbstractSynchronousLazy<IStorageWritePermissionArbitratorForOs>() {
		@Override
		protected IStorageWritePermissionArbitratorForOs create() {
			return new ExternalStorageWritePermissionsArbitratorForOs(context);
		}
	};

	private final CreateAndHold<IScanMediaFileBroadcaster> scanMediaFileBroadcasterLazy = new AbstractSynchronousLazy<IScanMediaFileBroadcaster>() {
		@Override
		protected IScanMediaFileBroadcaster create() {
			return new ScanMediaFileBroadcaster(context);
		}
	};

	private final AbstractSynchronousLazy<Intent> browseLibraryIntent = new AbstractSynchronousLazy<Intent>() {
		@Override
		protected final Intent create() {
			final Intent browseLibraryIntent = new Intent(context, BrowseLibraryActivity.class);
			browseLibraryIntent.setAction(BrowseLibraryActivity.showDownloadsAction);
			browseLibraryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			return browseLibraryIntent;
		}
	};

	private final CreateAndHold<NotificationManager> notificationManagerLazy = new AbstractSynchronousLazy<NotificationManager>() {
		@Override
		protected NotificationManager create() {
			return (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
		}
	};

	private final CreateAndHold<ChannelConfiguration> lazyChannelConfiguration = new AbstractSynchronousLazy<ChannelConfiguration>() {
		@Override
		protected ChannelConfiguration create() {
			return new SharedChannelProperties(context);
		}
	};
	private final CreateAndHold<String> lazyActiveNotificationChannelId = new AbstractSynchronousLazy<String>() {
		@Override
		protected String create() {
			final NotificationChannelActivator notificationChannelActivator = new NotificationChannelActivator(notificationManagerLazy.getObject());

			return notificationChannelActivator.activateChannel(lazyChannelConfiguration.getObject());
		}
	};

	private final Context context;
	private final ILibraryProvider libraryProvider;
	private final LocalBroadcastManager localBroadcastManager;
	private final BuildUrlProviders urlProviders;
	private final ProduceLibrarySyncHandlers librarySyncHandlersProduction;

	public StoredFileSynchronization(
		Context context,
		ILibraryProvider libraryProvider,
		LocalBroadcastManager localBroadcastManager,
		BuildUrlProviders urlProviders,
		ProduceLibrarySyncHandlers librarySyncHandlersProduction) {
		this.context = context;
		this.libraryProvider = libraryProvider;
		this.localBroadcastManager = localBroadcastManager;
		this.urlProviders = urlProviders;
		this.librarySyncHandlersProduction = librarySyncHandlersProduction;
	}

	@Override
	public Completable streamFileSynchronization() {
		logger.info("Starting sync.");

		setSyncNotificationText(null);
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
			.onErrorComplete(this::handleError);
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

	@NonNull
	private Notification buildSyncNotification(@Nullable String syncNotification) {
		final NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(context, lazyActiveNotificationChannelId.getObject());
		notifyBuilder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
		notifyBuilder.setContentTitle(context.getText(R.string.title_sync_files));
		if (syncNotification != null)
			notifyBuilder.setContentText(syncNotification);
		notifyBuilder.setContentIntent(PendingIntent.getActivity(context, 0, browseLibraryIntent.getObject(), 0));

		notifyBuilder.setOngoing(true);

		notifyBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

		return notifyBuilder.build();
	}

	private void setSyncNotificationText(@Nullable String syncNotification) {
		notificationManagerLazy.getObject().notify(notificationId, buildSyncNotification(syncNotification));
	}

	private void sendStoredFileBroadcast(@NonNull String action, @NonNull StoredFile storedFile) {
		final Intent storedFileBroadcastIntent = new Intent(action);
		storedFileBroadcastIntent.putExtra(storedFileEventKey, storedFile.getId());
		localBroadcastManager.sendBroadcast(storedFileBroadcastIntent);
	}
}
