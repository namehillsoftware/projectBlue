package com.lasthopesoftware.bluewater.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import androidx.work.*;
import com.annimon.stream.Stream;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.AccessConfigurationBuilder;
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.library.BrowseLibraryActivity;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.access.ItemProvider;
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
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.*;
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
import com.lasthopesoftware.bluewater.client.library.items.playlists.PlaylistItemFinder;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemsChecker;
import com.lasthopesoftware.bluewater.client.library.items.stored.conversion.StoredPlaylistItemsConverter;
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
import com.lasthopesoftware.bluewater.client.library.views.access.LibraryViewsByConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.views.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.lasthopesoftware.resources.notifications.notificationchannel.ChannelConfiguration;
import com.lasthopesoftware.resources.notifications.notificationchannel.NotificationChannelActivator;
import com.lasthopesoftware.resources.notifications.notificationchannel.SharedChannelProperties;
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;
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
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static android.content.Context.NOTIFICATION_SERVICE;

public class SyncWorker extends ListenableWorker {
	private static final Logger logger = LoggerFactory.getLogger(SyncWorker.class);

	private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(SyncWorker.class);

	public static final String onSyncStartEvent = magicPropertyBuilder.buildProperty("onSyncStartEvent");
	public static final String onSyncStopEvent = magicPropertyBuilder.buildProperty("onSyncStopEvent");
	public static final String onFileQueuedEvent = magicPropertyBuilder.buildProperty("onFileQueuedEvent");
	public static final String onFileDownloadingEvent = magicPropertyBuilder.buildProperty("onFileDownloadingEvent");
	public static final String onFileDownloadedEvent = magicPropertyBuilder.buildProperty("onFileDownloadedEvent");
	public static final String storedFileEventKey = magicPropertyBuilder.buildProperty("storedFileEventKey");

	private static final String workName = magicPropertyBuilder.buildProperty("");

	private static final int notificationId = 23;

	private final Context context;

	private final CreateAndHold<LocalBroadcastManager> localBroadcastManager = new AbstractSynchronousLazy<LocalBroadcastManager>() {
		@Override
		protected LocalBroadcastManager create() {
			return LocalBroadcastManager.getInstance(context);
		}
	};

	private final CreateAndHold<IStorageReadPermissionsRequestedBroadcast> storageReadPermissionsRequestedBroadcast = new Lazy<>(() -> new StorageReadPermissionsRequestedBroadcaster(localBroadcastManager.getObject()));
	private final CreateAndHold<IStorageWritePermissionsRequestedBroadcaster> storageWritePermissionsRequestedBroadcast = new Lazy<>(() -> new StorageWritePermissionsRequestedBroadcaster(localBroadcastManager.getObject()));

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

	private final Map<Integer, IConnectionProvider> libraryConnectionProviders = new ConcurrentHashMap<>();

	private final Lazy<IStoredFileSystemFileProducer> lazyStoredFileSystemFileProducer = new Lazy<>(StoredFileSystemFileProducer::new);
	private final Lazy<IServiceFileUriQueryParamsProvider> lazyServiceFileUriQueryParamsProvider = new Lazy<>(ServiceFileUriQueryParamsProvider::new);
	private final Lazy<IFileReadPossibleArbitrator> lazyFileReadPossibleArbitrator = new Lazy<>(FileReadPossibleArbitrator::new);
	private final Lazy<IFileWritePossibleArbitrator> lazyFileWritePossibleArbitrator = new Lazy<>(FileWritePossibleArbitrator::new);
	private final Lazy<IFileStreamWriter> lazyFileStreamWriter = new Lazy<>(FileStreamWriter::new);
	private final Lazy<ILibraryStorageReadPermissionsRequirementsProvider> lazyLibraryStorageReadPermissionsRequirementsProvider = new Lazy<>(LibraryStorageReadPermissionsRequirementsProvider::new);
	private final Lazy<ILibraryStorageWritePermissionsRequirementsProvider> lazyLibraryStorageWritePermissionsRequirementsProvider = new Lazy<>(LibraryStorageWritePermissionsRequirementsProvider::new);

	private final AtomicInteger librariesProcessing = new AtomicInteger();

	private final HashSet<LibrarySyncHandler> librarySyncHandlers = new HashSet<>();

	private final SettableFuture<Result> settableFuture = SettableFuture.create();

	private final OneParameterAction<LibrarySyncHandler> onLibrarySyncCompleteRunnable = librarySyncHandler -> {
		librarySyncHandlers.remove(librarySyncHandler);

		if (librariesProcessing.decrementAndGet() == 0) finishSync();
	};

	private final OneParameterAction<StoredFile> storedFileQueuedAction = storedFile -> sendStoredFileBroadcast(onFileQueuedEvent, storedFile);

	private final CreateAndHold<String> downloadingStatusLabel = new AbstractSynchronousLazy<String>() {
		@Override
		protected String create() {
			return context.getString(R.string.downloading_status_label);
		}
	};

	private final CreateAndHold<ILibraryProvider> lazyLibraryProvider = new AbstractSynchronousLazy<ILibraryProvider>() {
		@Override
		protected ILibraryProvider create() {
			return new LibraryRepository(context);
		}
	};

	private final CreateAndHold<OneParameterAction<StoredFile>> storedFileDownloadingAction = new AbstractSynchronousLazy<OneParameterAction<StoredFile>>() {
		@Override
		protected OneParameterAction<StoredFile> create() {
			return storedFile -> {
				sendStoredFileBroadcast(onFileDownloadingEvent, storedFile);

				final IConnectionProvider connectionProvider = libraryConnectionProviders.get(storedFile.getLibraryId());
				if (connectionProvider == null) return;

				final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
				final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertyCache,
					new FilePropertiesProvider(connectionProvider, filePropertyCache, ParsingScheduler.instance()));

				filePropertiesProvider.promiseFileProperties(new ServiceFile(storedFile.getServiceId()))
					.eventually(LoopedInPromise.response(new VoidResponse<>(fileProperties -> setSyncNotificationText(String.format(downloadingStatusLabel.getObject(), fileProperties.get(FilePropertiesProvider.NAME)))), context))
					.excuse(e -> LoopedInPromise.response(exception -> {
						setSyncNotificationText(String.format(downloadingStatusLabel.getObject(), context.getString(R.string.unknown_file)));
						return true;
					}, context).promiseResponse(e));
			};
		}
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

	private final CreateAndHold<StoredFilesChecker> lazyStoredFilesChecker = new AbstractSynchronousLazy<StoredFilesChecker>() {
		@Override
		protected StoredFilesChecker create() {
			return new StoredFilesChecker(new StoredFilesCounter(context));
		}
	};

	private final CreateAndHold<IStorageReadPermissionArbitratorForOs> lazyOsReadPermissions = new AbstractSynchronousLazy<IStorageReadPermissionArbitratorForOs>() {
		@Override
		protected IStorageReadPermissionArbitratorForOs create() {
			return new ExternalStorageReadPermissionsArbitratorForOs(context);
		}
	};

	private final CreateAndHold<LookupSyncDirectory> lazySyncDirectoryLookup = new AbstractSynchronousLazy<LookupSyncDirectory>() {
		@Override
		protected LookupSyncDirectory create() {
			return new SyncDirectoryLookup(
				new PublicDirectoryLookup(context),
				new PrivateDirectoryLookup(context));
		}
	};

	public static Operation syncImmediately() {
		final OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
		return WorkManager.getInstance().enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, oneTimeWorkRequest);
	}

	public static Operation scheduleSync() {
		final PeriodicWorkRequest.Builder periodicWorkRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 3, TimeUnit.HOURS);
		return WorkManager.getInstance()
			.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest.build());
	}

	public static Promise<Boolean> promiseIsSyncing() {
		return promiseWorkInfos()
			.then(workInfos -> Stream.of(workInfos).anyMatch(wi -> wi.getState() == WorkInfo.State.RUNNING));
	}

	public static Promise<Boolean> promiseIsScheduled() {
		return promiseWorkInfos()
			.then(workInfos -> Stream.of(workInfos).anyMatch(wi -> wi.getState() == WorkInfo.State.ENQUEUED));
	}

	public static Operation cancel() {
		return WorkManager.getInstance().cancelUniqueWork(workName);
	}

	private static Promise<List<WorkInfo>> promiseWorkInfos() {
		return new Promise<>(m -> {
			final ListenableFuture<List<WorkInfo>> workInfosByName = WorkManager.getInstance().getWorkInfosForUniqueWork(workName);
			m.cancellationRequested(() -> workInfosByName.cancel(false));
			workInfosByName.addListener(() -> {
				try {
					m.sendResolution(workInfosByName.get());
				} catch (ExecutionException e) {
					final Throwable cause = e.getCause();
					m.sendRejection(cause != null ? cause : e);
				} catch (InterruptedException e) {
					m.sendRejection(e);
				}
			}, AsyncTask.THREAD_POOL_EXECUTOR);
		});
	}

	public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
		this.context = context;
	}

	@NonNull
	@Override
	public ListenableFuture<Result> startWork() {
		if (!isDeviceStateValidForSync()) {
			finishSync();
			return settableFuture;
		}

		logger.info("Starting sync.");

		setSyncNotificationText(null);
		localBroadcastManager.getObject().sendBroadcast(new Intent(onSyncStartEvent));

		lazyLibraryProvider.getObject().getAllLibraries().then(new VoidResponse<>(libraries -> {
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
							.then(new VoidResponse<>(urlProvider -> {
								if (urlProvider == null) {
									if (librariesProcessing.decrementAndGet() == 0) finishSync();
									return;
								}

								final ConnectionProvider connectionProvider = new ConnectionProvider(urlProvider, OkHttpFactory.getInstance());
								libraryConnectionProviders.put(library.getId(), connectionProvider);

								final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(connectionProvider, filePropertyCache, ParsingScheduler.instance());
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
								librarySyncHandler.setOnFileDownloading(storedFileDownloadingAction.getObject());
								librarySyncHandler.setOnFileDownloaded(storedFileDownloadedAction);
								librarySyncHandler.setOnQueueProcessingCompleted(onLibrarySyncCompleteRunnable);
								librarySyncHandler.setOnFileReadError(storedFileReadErrorAction);
								librarySyncHandler.setOnFileWriteError(storedFileWriteErrorAction);
								librarySyncHandler.startSync();

								librarySyncHandlers.add(librarySyncHandler);
							}));

					promiseLibrarySyncStarted
						.excuse(new VoidResponse<>(e ->
							logger.error("There was an error getting the URL for library ID " + library.getId(), e)));

					return promiseLibrarySyncStarted;
				})
				.excuse(e -> {
					if (librariesProcessing.decrementAndGet() == 0) finishSync();
					return null;
				});
			}
		}));

		return settableFuture;
	}

	@Override
	public void onStopped() {
		cancelSync();
	}

	private boolean isDeviceStateValidForSync() {
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

		final boolean isSyncOnWifiOnly = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnWifiOnlyKey, false);
		if (isSyncOnWifiOnly) {
			if (!IoCommon.isWifiConnected(context)) return false;

			context.registerReceiver(onWifiStateChangedReceiver.getObject(), new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		}

		final boolean isSyncOnPowerOnly = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, false);
		if (isSyncOnPowerOnly) {
			if (!IoCommon.isPowerConnected(context)) return false;

			context.registerReceiver(onPowerDisconnectedReceiver.getObject(), new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
		}

		return true;
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
		localBroadcastManager.getObject().sendBroadcast(storedFileBroadcastIntent);
	}

	private void cancelSync() {
		Stream.of(librarySyncHandlers).forEach(LibrarySyncHandler::cancel);
	}

	private void finishSync() {
		settableFuture.set(Result.success());

		localBroadcastManager.getObject().sendBroadcast(new Intent(onSyncStopEvent));
	}
}
