package com.lasthopesoftware.bluewater.client.stored.worker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders;
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester;
import com.lasthopesoftware.bluewater.client.library.BrowseLibraryActivity;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.broadcasts.IScanMediaFileBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.broadcasts.ScanMediaFileBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.io.FileStreamWriter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.read.IStorageReadPermissionsRequestedBroadcast;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.read.StorageReadPermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write.IStorageWritePermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write.StorageWritePermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.LibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.LibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection;
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncHandler;
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup;
import com.lasthopesoftware.bluewater.client.stored.library.sync.factory.LibrarySyncHandlerFactory;
import com.lasthopesoftware.bluewater.client.stored.library.sync.factory.ProduceLibrarySyncHandlers;
import com.lasthopesoftware.bluewater.client.stored.worker.constraints.SyncWorkerConstraints;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.observables.ObservedPromise;
import com.lasthopesoftware.bluewater.shared.observables.StreamedPromise;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.lasthopesoftware.resources.notifications.notificationchannel.ChannelConfiguration;
import com.lasthopesoftware.resources.notifications.notificationchannel.NotificationChannelActivator;
import com.lasthopesoftware.resources.notifications.notificationchannel.SharedChannelProperties;
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;
import com.lasthopesoftware.storage.directories.PrivateDirectoryLookup;
import com.lasthopesoftware.storage.directories.PublicDirectoryLookup;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.lasthopesoftware.storage.write.permissions.ExternalStorageWritePermissionsArbitratorForOs;
import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator;
import com.lasthopesoftware.storage.write.permissions.IStorageWritePermissionArbitratorForOs;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
	private final ILibraryProvider libraryProvider;
	private final LocalBroadcastManager localBroadcastManager;
	private final BuildUrlProviders urlProviders;
	private final ProduceLibrarySyncHandlers librarySyncHandlersProduction;

	public static Operation syncImmediately(Context context) {
		final OneTimeWorkRequest.Builder oneTimeWorkRequest = new OneTimeWorkRequest.Builder(SyncWorker.class);
		oneTimeWorkRequest.setConstraints(constraints(context));
		return WorkManager.getInstance().enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, oneTimeWorkRequest.build());
	}

	public static Operation scheduleSync(Context context) {
		final PeriodicWorkRequest.Builder periodicWorkRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 3, TimeUnit.HOURS);
		periodicWorkRequest.setConstraints(constraints(context));
		return WorkManager.getInstance()
			.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest.build());
	}

	private static Constraints constraints(Context context) {
		final SharedPreferences manager = PreferenceManager.getDefaultSharedPreferences(context);
		return new SyncWorkerConstraints(manager).getCurrentConstraints();
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
				} catch (ExecutionException | InterruptedException e) {
					m.sendRejection(e);
				}
			}, AsyncTask.THREAD_POOL_EXECUTOR);
		});
	}

	private static final int notificationId = 23;

	private final Context context;

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

	private final HashSet<LibrarySyncHandler> librarySyncHandlers = new HashSet<>();

	private final SettableFuture<Result> settableFuture = SettableFuture.create();

	private final OneParameterAction<StoredFile> storedFileQueuedAction = storedFile -> sendStoredFileBroadcast(onFileQueuedEvent, storedFile);

	private final CreateAndHold<String> downloadingStatusLabel = new AbstractSynchronousLazy<String>() {
		@Override
		protected String create() {
			return context.getString(R.string.downloading_status_label);
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

	private final OneParameterAction<StoredFileJobStatus> storedFileDownloadedAction = storedFileJobResult -> {
		sendStoredFileBroadcast(onFileDownloadedEvent, storedFileJobResult.storedFile);

		if (storedFileJobResult.storedFileJobState == StoredFileJobState.Downloaded)
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

	public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		this(
			context,
			workerParams,
			new LibraryRepository(context),
			LocalBroadcastManager.getInstance(context),
			new UrlScanner(
				new ConnectionTester(),
				new ServerLookup(new ServerInfoXmlRequest(new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build())),
				OkHttpFactory.getInstance()),
			new LibrarySyncHandlerFactory(
				new StoredFileAccess(context, new StoredFilesCollection(context)),
				context,
				new ExternalStorageReadPermissionsArbitratorForOs(context),
				new SyncDirectoryLookup(
					new PublicDirectoryLookup(context),
					new PrivateDirectoryLookup(context)),
				new StoredFileSystemFileProducer(),
				new ServiceFileUriQueryParamsProvider(),
				new FileReadPossibleArbitrator(),
				new FileWritePossibleArbitrator(),
				new FileStreamWriter(),
				new LibraryStorageWritePermissionsRequirementsProvider(),
				new LibraryStorageReadPermissionsRequirementsProvider()));
	}

	public SyncWorker(
		@NonNull Context context,
		@NonNull WorkerParameters workerParams,
		ILibraryProvider libraryProvider,
		LocalBroadcastManager localBroadcastManager,
		BuildUrlProviders urlProviders,
		ProduceLibrarySyncHandlers librarySyncHandlersProduction) {
		super(context, workerParams);
		this.context = context;
		this.libraryProvider = libraryProvider;
		this.localBroadcastManager = localBroadcastManager;
		this.urlProviders = urlProviders;
		this.librarySyncHandlersProduction = librarySyncHandlersProduction;
	}

	@NonNull
	@Override
	public ListenableFuture<Result> startWork() {
		logger.info("Starting sync.");

		setSyncNotificationText(null);
		localBroadcastManager.sendBroadcast(new Intent(onSyncStartEvent));

		StreamedPromise.stream(libraryProvider.getAllLibraries())
			.map(library -> {
				if (library.isSyncLocalConnectionsOnly()) library.setLocalOnly(true);

				return ObservedPromise.observe(urlProviders.promiseBuiltUrlProvider(library)
					.then(urlProvider -> librarySyncHandlersProduction.getNewSyncHandler(urlProvider, library)));
			})
			.flatMap(o -> o.flatMap(LibrarySyncHandler::observeLibrarySync))
			.subscribe(new Observer<StoredFileJobStatus>() {
				@Override
				public void onSubscribe(Disposable d) {

				}

				@Override
				public void onNext(StoredFileJobStatus storedFileJobStatus) {

				}

				@Override
				public void onError(Throwable e) {
					settableFuture.setException(e);
				}

				@Override
				public void onComplete() {
					finishSync();
				}
			});

		return settableFuture;
	}

	@Override
	public void onStopped() {
		cancelSync();
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

	private void cancelSync() {
		Stream.of(librarySyncHandlers).forEach(LibrarySyncHandler::cancel);
	}

	private void finishSync() {
		settableFuture.set(Result.success());

		localBroadcastManager.sendBroadcast(new Intent(onSyncStopEvent));
	}
}
