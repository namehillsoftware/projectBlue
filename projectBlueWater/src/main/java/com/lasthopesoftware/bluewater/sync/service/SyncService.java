package com.lasthopesoftware.bluewater.sync.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.AccessConfigurationBuilder;
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.BrowseLibraryActivity;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.FileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.broadcasts.IScanMediaFileBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.broadcasts.ScanMediaFileBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.io.FileStreamWriter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.io.IFileStreamWriter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFilesChecker;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFilesCounter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResult;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResultOptions;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.retrieval.StoredFileQuery;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.retrieval.StoredFilesCollection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.MediaFileIdProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.MediaQueryCursorProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.updates.StoredFileUpdater;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemsChecker;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.read.IStorageReadPermissionsRequestedBroadcast;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.read.StorageReadPermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write.IStorageWritePermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write.StorageWritePermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.ILibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.LibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.ILibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.LibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.sync.LibrarySyncHandler;
import com.lasthopesoftware.bluewater.client.library.sync.LookupSyncDirectory;
import com.lasthopesoftware.bluewater.client.library.sync.SyncDirectoryLookup;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.lasthopesoftware.bluewater.sync.receivers.SyncAlarmBroadcastReceiver;
import com.lasthopesoftware.resources.notifications.notificationchannel.ChannelConfiguration;
import com.lasthopesoftware.resources.notifications.notificationchannel.NotificationChannelActivator;
import com.lasthopesoftware.resources.notifications.notificationchannel.SharedChannelProperties;
import com.lasthopesoftware.storage.directories.PrivateDirectoryLookup;
import com.lasthopesoftware.storage.directories.PublicDirectoryLookup;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.lasthopesoftware.storage.write.permissions.ExternalStorageWritePermissionsArbitratorForOs;
import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.lasthopesoftware.storage.write.permissions.IStorageWritePermissionArbitratorForOs;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.namehillsoftware.handoff.promises.response.ImmediateAction.perform;

public class SyncService extends Service {

	public static final String onSyncStartEvent = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "onSyncStartEvent");
	public static final String onSyncStopEvent = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "onSyncStopEvent");
	public static final String onFileQueuedEvent = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "onFileQueuedEvent");
	public static final String onFileDownloadingEvent = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "onFileDownloadingEvent");
	public static final String onFileDownloadedEvent = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "onFileDownloadedEvent");
	public static final String storedFileEventKey = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "storedFileEventKey");

	private static final String doSyncAction = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "doSyncAction");
	private static final String cancelSyncAction = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "cancelSyncAction");
	private static final long syncInterval = 3 * 60 * 60 * 1000; // 3 hours
	private static final int notificationId = 23;

	private static final Logger logger = LoggerFactory.getLogger(SyncService.class);

	private static volatile boolean isSyncRunning;

	public static boolean isSyncScheduled(Context context) {
		return PendingIntent.getBroadcast(context, 0, new Intent(SyncAlarmBroadcastReceiver.scheduledSyncIntent), PendingIntent.FLAG_NO_CREATE) != null;
	}

	public static boolean isSyncRunning() {
		return isSyncRunning;
	}

	public static void doSync(Context context) {
		final Intent intent = new Intent(context, SyncService.class);
		intent.setAction(doSyncAction);

		context.startService(intent);
	}

	public static void cancelSync(Context context) {
		final Intent intent = new Intent(context, SyncService.class);
		intent.setAction(cancelSyncAction);

		context.startService(intent);
	}

	private final Lazy<LocalBroadcastManager> localBroadcastManager = new Lazy<>(() -> LocalBroadcastManager.getInstance(this));

	private final Lazy<IStorageReadPermissionsRequestedBroadcast> storageReadPermissionsRequestedBroadcast = new Lazy<>(() -> new StorageReadPermissionsRequestedBroadcaster(localBroadcastManager.getObject()));
	private final Lazy<IStorageWritePermissionsRequestedBroadcaster> storageWritePermissionsRequestedBroadcast = new Lazy<>(() -> new StorageWritePermissionsRequestedBroadcaster(localBroadcastManager.getObject()));

	private final Lazy<IStorageReadPermissionArbitratorForOs> storageReadPermissionArbitratorForOsLazy = new Lazy<>(() -> new ExternalStorageReadPermissionsArbitratorForOs(this));
	private final Lazy<IStorageWritePermissionArbitratorForOs> storageWritePermissionArbitratorForOsLazy = new Lazy<>(() -> new ExternalStorageWritePermissionsArbitratorForOs(this));

	private final Lazy<IScanMediaFileBroadcaster> scanMediaFileBroadcasterLazy = new Lazy<>(() -> new ScanMediaFileBroadcaster(this));

	private final Map<Integer, IConnectionProvider> libraryConnectionProviders = new ConcurrentHashMap<>();

	private final Lazy<IStoredFileSystemFileProducer> lazyStoredFileSystemFileProducer = new Lazy<>(StoredFileSystemFileProducer::new);
	private final Lazy<IServiceFileUriQueryParamsProvider> lazyServiceFileUriQueryParamsProvider = new Lazy<>(ServiceFileUriQueryParamsProvider::new);
	private final Lazy<IFileReadPossibleArbitrator> lazyFileReadPossibleArbitrator = new Lazy<>(FileReadPossibleArbitrator::new);
	private final Lazy<IFileWritePossibleArbitrator> lazyFileWritePossibleArbitrator = new Lazy<>(FileWritePossibleArbitrator::new);
	private final Lazy<IFileStreamWriter> lazyFileStreamWriter = new Lazy<>(FileStreamWriter::new);
	private final Lazy<ILibraryStorageReadPermissionsRequirementsProvider> lazyLibraryStorageReadPermissionsRequirementsProvider = new Lazy<>(LibraryStorageReadPermissionsRequirementsProvider::new);
	private final Lazy<ILibraryStorageWritePermissionsRequirementsProvider> lazyLibraryStorageWritePermissionsRequirementsProvider = new Lazy<>(LibraryStorageWritePermissionsRequirementsProvider::new);

	private PowerManager.WakeLock wakeLock;

	private final AtomicInteger librariesProcessing = new AtomicInteger();

	private final HashSet<LibrarySyncHandler> librarySyncHandlers = new HashSet<>();

	private final OneParameterAction<LibrarySyncHandler> onLibrarySyncCompleteRunnable = librarySyncHandler -> {
		librarySyncHandlers.remove(librarySyncHandler);

		if (librariesProcessing.decrementAndGet() == 0) finishSync();
	};

	private final OneParameterAction<StoredFile> storedFileQueuedAction = storedFile -> sendStoredFileBroadcast(onFileQueuedEvent, storedFile);

	private final Lazy<String> downloadingStatusLabel = new Lazy<>(() -> getString(R.string.downloading_status_label));

	private final CreateAndHold<ILibraryProvider> lazyLibraryProvider = new Lazy<ILibraryProvider>(() -> new LibraryRepository(SyncService.this));

	private final OneParameterAction<StoredFile> storedFileDownloadingAction = storedFile -> {
		sendStoredFileBroadcast(onFileDownloadingEvent, storedFile);

		final IConnectionProvider connectionProvider = libraryConnectionProviders.get(storedFile.getLibraryId());
		if (connectionProvider == null) return;

		final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
		final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertyCache, new FilePropertiesProvider(connectionProvider, filePropertyCache));

		filePropertiesProvider.promiseFileProperties(new ServiceFile(storedFile.getServiceId()))
			.eventually(LoopedInPromise.response(perform(fileProperties -> setSyncNotificationText(String.format(downloadingStatusLabel.getObject(), fileProperties.get(FilePropertiesProvider.NAME)))), this))
			.excuse(e -> LoopedInPromise.response(exception -> {
				setSyncNotificationText(String.format(downloadingStatusLabel.getObject(), getString(R.string.unknown_file)));
				return true;
			}, this).promiseResponse(e));
	};

	private final OneParameterAction<StoredFileJobResult> storedFileDownloadedAction = storedFileJobResult -> {
		sendStoredFileBroadcast(onFileDownloadedEvent, storedFileJobResult.storedFile);

		if (storedFileJobResult.storedFileJobResult == StoredFileJobResultOptions.Downloaded)
			scanMediaFileBroadcasterLazy.getObject().sendScanMediaFileBroadcastForFile(storedFileJobResult.downloadedFile);
	};

	private final TwoParameterAction<Library, StoredFile> storedFileReadErrorAction = (library, storedFile) -> {
		if (!storageReadPermissionArbitratorForOsLazy.getObject().isReadPermissionGranted())
			storageReadPermissionsRequestedBroadcast.getObject().sendReadPermissionsRequestedBroadcast(library.getId());
	};

	private final TwoParameterAction<Library, StoredFile> storedFileWriteErrorAction = (library, storedFile) -> {
		if (!storageWritePermissionArbitratorForOsLazy.getObject().isWritePermissionGranted())
			storageWritePermissionsRequestedBroadcast.getObject().sendWritePermissionsNeededBroadcast(library.getId());
	};

	private final AbstractSynchronousLazy<BroadcastReceiver> onWifiStateChangedReceiver = new AbstractSynchronousLazy<BroadcastReceiver>() {
		@Override
		protected final BroadcastReceiver create() {
			return new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (!IoCommon.isWifiConnected(context)) cancelSync();
				}
			};
		}
	};

	private final AbstractSynchronousLazy<BroadcastReceiver> onPowerDisconnectedReceiver = new AbstractSynchronousLazy<BroadcastReceiver>() {
		@Override
		public final BroadcastReceiver create() {
			return new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					cancelSync();
				}
			};
		}
	};

	private final AbstractSynchronousLazy<Intent> browseLibraryIntent = new AbstractSynchronousLazy<Intent>() {
		@Override
		protected final Intent create() {
			final Intent browseLibraryIntent = new Intent(SyncService.this, BrowseLibraryActivity.class);
			browseLibraryIntent.setAction(BrowseLibraryActivity.showDownloadsAction);
			browseLibraryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			return browseLibraryIntent;
		}
	};

	private final CreateAndHold<NotificationManager> notificationManagerLazy = new Lazy<>(() -> (NotificationManager) getSystemService(NOTIFICATION_SERVICE));

	private final CreateAndHold<ChannelConfiguration> lazyChannelConfiguration = new Lazy<>(() -> new SharedChannelProperties(this));
	private final CreateAndHold<String> lazyActiveNotificationChannelId = new AbstractSynchronousLazy<String>() {
		@Override
		protected String create() {
			final NotificationChannelActivator notificationChannelActivator = new NotificationChannelActivator(notificationManagerLazy.getObject());

			return notificationChannelActivator.activateChannel(lazyChannelConfiguration.getObject());
		}
	};

	private final CreateAndHold<StoredFilesChecker> lazyStoredFilesChecker = new Lazy<>(() -> new StoredFilesChecker(new StoredFilesCounter(this)));

	private final CreateAndHold<IStorageReadPermissionArbitratorForOs> lazyOsReadPermissions = new Lazy<>(() -> new ExternalStorageReadPermissionsArbitratorForOs(this));

	private final CreateAndHold<LookupSyncDirectory> lazySyncDirectoryLookup = new Lazy<>(() -> new SyncDirectoryLookup(
		new PublicDirectoryLookup(this),
		new PrivateDirectoryLookup(this)));

	@Override
	public void onCreate() {
		super.onCreate();

		final PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "wakeLock"));
		wakeLock.acquire();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String action = intent.getAction();

		if (cancelSyncAction.equals(action)) cancelSync();

		if (!doSyncAction.equals(action)) return START_REDELIVER_INTENT;

		final int result = START_NOT_STICKY;

		if (!isDeviceStateValidForSync()) {
			finishSync();
			return result;
		}

		final Context context = this;

		logger.info("Starting sync.");

		isSyncRunning = true;
		setSyncNotificationText(null);
		localBroadcastManager.getObject().sendBroadcast(new Intent(onSyncStartEvent));

		lazyLibraryProvider.getObject().getAllLibraries().then(perform(libraries -> {
			librariesProcessing.set(libraries.size());

			if (librariesProcessing.get() == 0) {
				finishSync();
				return;
			}

			final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();

			for (final Library library : libraries) {
				if (library.isSyncLocalConnectionsOnly()) {
					library.setLocalOnly(true);
				}

				final StoredItemAccess storedItemAccess = new StoredItemAccess(context, library);
				final StoredItemsChecker storedItemsChecker = new StoredItemsChecker(storedItemAccess, lazyStoredFilesChecker.getObject());

				storedItemsChecker.promiseIsAnyStoredItemsOrFiles(library).eventually(isAny -> {
					if (!isAny) {
						if (librariesProcessing.decrementAndGet() == 0) finishSync();
						return Promise.empty();
					}

					final Promise<Void> promiseLibrarySyncStarted =
						AccessConfigurationBuilder.buildConfiguration(context, library)
							.then(perform(urlProvider -> {
								if (urlProvider == null) {
									if (librariesProcessing.decrementAndGet() == 0) finishSync();
									return;
								}

								final ConnectionProvider connectionProvider = new ConnectionProvider(urlProvider);
								libraryConnectionProviders.put(library.getId(), connectionProvider);

								final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(connectionProvider, filePropertyCache);
								final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertyCache, filePropertiesProvider);

								final StoredFileAccess storedFileAccess = new StoredFileAccess(
									context,
									new StoredFilesCollection(context));

								final MediaQueryCursorProvider cursorProvider = new MediaQueryCursorProvider(
									context,
									cachedFilePropertiesProvider);

								final StoredFileUpdater storedFileUpdater = new StoredFileUpdater(
									context,
									new MediaFileUriProvider(
										context,
										cursorProvider,
										lazyOsReadPermissions.getObject(),
										library,
										true),
									new MediaFileIdProvider(
										cursorProvider,
										lazyOsReadPermissions.getObject()),
									new StoredFileQuery(context),
									cachedFilePropertiesProvider,
									lazySyncDirectoryLookup.getObject());

								final LibrarySyncHandler librarySyncHandler =
									new LibrarySyncHandler(library,
										new StoredItemServiceFileCollector(storedItemAccess, new FileProvider(new FileStringListProvider(connectionProvider))),
										storedFileAccess,
										storedFileUpdater,
										new StoredFileDownloader(
											lazyStoredFileSystemFileProducer.getObject(),
											connectionProvider,
											storedFileAccess,
											lazyServiceFileUriQueryParamsProvider.getObject(),
											lazyFileReadPossibleArbitrator.getObject(),
											lazyFileWritePossibleArbitrator.getObject(),
											lazyFileStreamWriter.getObject()),
										lazyLibraryStorageReadPermissionsRequirementsProvider.getObject(),
										lazyLibraryStorageWritePermissionsRequirementsProvider.getObject());

								librarySyncHandler.setOnFileQueued(storedFileQueuedAction);
								librarySyncHandler.setOnFileDownloading(storedFileDownloadingAction);
								librarySyncHandler.setOnFileDownloaded(storedFileDownloadedAction);
								librarySyncHandler.setOnQueueProcessingCompleted(onLibrarySyncCompleteRunnable);
								librarySyncHandler.setOnFileReadError(storedFileReadErrorAction);
								librarySyncHandler.setOnFileWriteError(storedFileWriteErrorAction);
								librarySyncHandler.startSync();

								librarySyncHandlers.add(librarySyncHandler);
							}));

					promiseLibrarySyncStarted
						.excuse(perform(e ->
							logger.error("There was an error getting the URL for library ID " + library.getId(), e)));

					return promiseLibrarySyncStarted;
				})
				.excuse(e -> {
					if (librariesProcessing.decrementAndGet() == 0) finishSync();
					return null;
				});
			}
		}));

		return result;
	}

	private boolean isDeviceStateValidForSync() {
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		final boolean isSyncOnWifiOnly = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnWifiOnlyKey, false);
		if (isSyncOnWifiOnly) {
			if (!IoCommon.isWifiConnected(this)) return false;

			registerReceiver(onWifiStateChangedReceiver.getObject(), new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		}

		final boolean isSyncOnPowerOnly = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, false);
		if (isSyncOnPowerOnly) {
			if (!IoCommon.isPowerConnected(this)) return false;

			registerReceiver(onPowerDisconnectedReceiver.getObject(), new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
		}

		return true;
	}

	@NonNull
	private Notification buildSyncNotification(@Nullable String syncNotification) {
		final NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this, lazyActiveNotificationChannelId.getObject());
		notifyBuilder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
		notifyBuilder.setContentTitle(getText(R.string.title_sync_files));
		if (syncNotification != null)
			notifyBuilder.setContentText(syncNotification);
		notifyBuilder.setContentIntent(PendingIntent.getActivity(this, 0, browseLibraryIntent.getObject(), 0));

		notifyBuilder.setOngoing(true);

		notifyBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

		return notifyBuilder.build();
	}

	private void setSyncNotificationText(@Nullable String syncNotification) {
		startForeground(notificationId, buildSyncNotification(syncNotification));
	}

	private void sendStoredFileBroadcast(@NonNull String action, @NonNull StoredFile storedFile) {
		final Intent storedFileBroadcastIntent = new Intent(action);
		storedFileBroadcastIntent.putExtra(storedFileEventKey, storedFile.getId());
		localBroadcastManager.getObject().sendBroadcast(storedFileBroadcastIntent);
	}

	private void cancelSync() {
		Stream.of(librarySyncHandlers).forEach(LibrarySyncHandler::cancel);
	}

	private void finishSync() {
		logger.info("Finishing sync. Scheduling next sync for " + syncInterval + "ms from now.");

		// Set an alarm for the then time we runWith this bad boy
		final AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, new Intent(SyncAlarmBroadcastReceiver.scheduledSyncIntent), PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + syncInterval, pendingIntent);

		stopForeground(true);
		stopSelf();

		isSyncRunning = false;
		localBroadcastManager.getObject().sendBroadcast(new Intent(onSyncStopEvent));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (onWifiStateChangedReceiver.isCreated())
			unregisterReceiver(onWifiStateChangedReceiver.getObject());

		if (onPowerDisconnectedReceiver.isCreated())
			unregisterReceiver(onPowerDisconnectedReceiver.getObject());

		wakeLock.release();
	}

	@Override
	public IBinder onBind(Intent intent) { return lazyBinder.getObject(); }

	private final Lazy<IBinder> lazyBinder = new Lazy<>(() -> new GenericBinder<>(this));
}
