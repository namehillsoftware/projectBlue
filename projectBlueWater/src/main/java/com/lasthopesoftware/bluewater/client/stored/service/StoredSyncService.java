package com.lasthopesoftware.bluewater.client.stored.service;

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
import android.preference.PreferenceManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.LibraryFileProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.LibraryFileStringListProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.broadcasts.ScanMediaFileBroadcaster;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.io.FileStreamWriter;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.request.read.StorageReadPermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.browsing.library.request.write.StorageWritePermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner;
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup;
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.stored.library.SyncLibraryProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaFileIdProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaQueryCursorProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater;
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncHandler;
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup;
import com.lasthopesoftware.bluewater.client.stored.service.notifications.PostSyncNotification;
import com.lasthopesoftware.bluewater.client.stored.service.notifications.SyncChannelProperties;
import com.lasthopesoftware.bluewater.client.stored.service.receivers.SyncStartedReceiver;
import com.lasthopesoftware.bluewater.client.stored.service.receivers.file.ReceiveStoredFileEvent;
import com.lasthopesoftware.bluewater.client.stored.service.receivers.file.StoredFileBroadcastReceiver;
import com.lasthopesoftware.bluewater.client.stored.service.receivers.file.StoredFileDownloadingNotifier;
import com.lasthopesoftware.bluewater.client.stored.service.receivers.file.StoredFileMediaScannerNotifier;
import com.lasthopesoftware.bluewater.client.stored.service.receivers.file.StoredFileReadPermissionsReceiver;
import com.lasthopesoftware.bluewater.client.stored.service.receivers.file.StoredFileWritePermissionsReceiver;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;
import com.lasthopesoftware.bluewater.client.stored.sync.SynchronizeStoredFiles;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.resources.network.ActiveNetworkFinder;
import com.lasthopesoftware.resources.notifications.NoOpChannelActivator;
import com.lasthopesoftware.resources.notifications.notificationchannel.ActivateChannel;
import com.lasthopesoftware.resources.notifications.notificationchannel.ChannelConfiguration;
import com.lasthopesoftware.resources.notifications.notificationchannel.NotificationChannelActivator;
import com.lasthopesoftware.resources.strings.Base64Encoder;
import com.lasthopesoftware.storage.directories.PrivateDirectoryLookup;
import com.lasthopesoftware.storage.directories.PublicDirectoryLookup;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.lasthopesoftware.storage.write.permissions.ExternalStorageWritePermissionsArbitratorForOs;
import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.Disposable;
import okhttp3.OkHttpClient;

import static androidx.core.app.NotificationCompat.CATEGORY_PROGRESS;

public class StoredSyncService extends Service implements PostSyncNotification {

	private static final Logger logger = LoggerFactory.getLogger(StoredSyncService.class);

	private static final String doSyncAction = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService.class, "doSyncAction");
	private static final String isUninterruptedSyncSetting = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService.class, "isUninterruptedSyncSetting");
	private static final String cancelSyncAction = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService.class, "cancelSyncAction");
	private static final String lastSyncTime = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService.class, "lastSyncTime");

	private static final int notificationId = 23;

	private static final int buildConnectionTimeoutTime = 10000;

	private static Disposable synchronizationDisposable;

	public static void doSync(Context context) {
		final Intent intent = new Intent(context, StoredSyncService.class);
		intent.setAction(doSyncAction);

		safelyStartService(context, intent);
	}

	public static void doSyncUninterrupted(Context context) {
		final Intent intent = new Intent(context, StoredSyncService.class);
		intent.setAction(doSyncAction);
		intent.putExtra(isUninterruptedSyncSetting, true);

		safelyStartService(context, intent);
	}

	public static void cancelSync(Context context) {
		context.startService(getSelfIntent(context, cancelSyncAction));
	}

	public static boolean isSyncRunning() {
		return synchronizationDisposable != null;
	}

	private static void safelyStartService(Context context, Intent intent) {
		try {
			ContextCompat.startForegroundService(context, intent);
		} catch (IllegalStateException e) {
			logger.warn("An illegal state exception occurred while trying to start the service", e);
		} catch (SecurityException e) {
			logger.warn("A security exception occurred while trying to start the service", e);
		}
	}

	private static Intent getSelfIntent(Context context, String action) {
		final Intent intent = new Intent(context, StoredSyncService.class);
		intent.setAction(action);

		return intent;
	}

	private final AbstractSynchronousLazy<SharedPreferences> lazySharedPreferences = new Lazy<>(() -> PreferenceManager.getDefaultSharedPreferences(this));

	private final AbstractSynchronousLazy<BroadcastReceiver> onWifiStateChangedReceiver = new AbstractSynchronousLazy<BroadcastReceiver>() {
		@Override
		protected final BroadcastReceiver create() {
			return new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					final boolean isSyncOnWifiOnly = lazySharedPreferences.getObject().getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnWifiOnlyKey, false);
					if (isSyncOnWifiOnly && !IoCommon.isWifiConnected(context))
						cancelSync(StoredSyncService.this);
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
					final boolean isSyncOnPowerOnly = lazySharedPreferences.getObject().getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, false);
					if (isSyncOnPowerOnly)
						cancelSync(StoredSyncService.this);
				}
			};
		}
	};

	private final CreateAndHold<String> lazyActiveNotificationChannelId = new AbstractSynchronousLazy<String>() {
		@Override
		protected String create() {
			final ActivateChannel notificationChannelActivator = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
				? new NotificationChannelActivator(notificationManagerLazy.getObject())
				: new NoOpChannelActivator();

			return notificationChannelActivator.activateChannel(lazyChannelConfiguration.getObject());
		}
	};

	private final AbstractSynchronousLazy<Intent> browseLibraryIntent = new AbstractSynchronousLazy<Intent>() {
		@Override
		protected final Intent create() {
			final Intent browseLibraryIntent = new Intent(StoredSyncService.this, BrowserEntryActivity.class);
			browseLibraryIntent.setAction(BrowserEntryActivity.showDownloadsAction);
			browseLibraryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			return browseLibraryIntent;
		}
	};

	private final CreateAndHold<NotificationManager> notificationManagerLazy = new AbstractSynchronousLazy<NotificationManager>() {
		@Override
		protected NotificationManager create() {
			return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		}
	};

	private final CreateAndHold<ChannelConfiguration> lazyChannelConfiguration = new AbstractSynchronousLazy<ChannelConfiguration>() {
		@Override
		protected ChannelConfiguration create() {
			return new SyncChannelProperties(StoredSyncService.this);
		}
	};

	private final CreateAndHold<LocalBroadcastManager> lazyBroadcastManager = new Lazy<>(() -> LocalBroadcastManager.getInstance(this));

	private final CreateAndHold<UrlScanner> lazyUrlScanner = new AbstractSynchronousLazy<UrlScanner>() {
		@Override
		protected UrlScanner create() {
			final OkHttpClient client = new OkHttpClient.Builder()
				.connectTimeout(buildConnectionTimeoutTime, TimeUnit.MILLISECONDS)
				.build();

			return new UrlScanner(
				new Base64Encoder(),
				new ConnectionTester(),
				new ServerLookup(new ServerInfoXmlRequest(lazyLibraryRepository.getObject(), client)),
				OkHttpFactory.getInstance());
		}
	};

	private final CreateAndHold<IStoredFileAccess> lazyStoredFileAccess = new Lazy<>(() -> new StoredFileAccess(this, new StoredFilesCollection(this)));

	private final CreateAndHold<IStorageReadPermissionArbitratorForOs> lazyReadPermissionArbitratorForOs = new Lazy<>(() -> new ExternalStorageReadPermissionsArbitratorForOs(this));

	private final CreateAndHold<ILibraryProvider> lazyLibraryRepository = new Lazy<>(() -> new LibraryRepository(this));

	private final CreateAndHold<ISelectedLibraryIdentifierProvider> lazyLibraryIdentifierProvider = new Lazy<>(() -> new SelectedBrowserLibraryIdentifierProvider(this));

	private final CreateAndHold<ProvideLibraryConnections> lazyLibraryConnections = new AbstractSynchronousLazy<ProvideLibraryConnections>() {
		@Override
		protected ProvideLibraryConnections create() {
			return new LibraryConnectionProvider(
				new SyncLibraryProvider(lazyLibraryRepository.getObject()),
				new LiveUrlProvider(
					new ActiveNetworkFinder(StoredSyncService.this),
					lazyUrlScanner.getObject()),
				new ConnectionTester(),
				OkHttpFactory.getInstance());
		}
	};

	private final CreateAndHold<CachedFilePropertiesProvider> lazyFileProperties = new AbstractSynchronousLazy<CachedFilePropertiesProvider>() {
		@Override
		protected CachedFilePropertiesProvider create() {
			final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
			return new CachedFilePropertiesProvider(
				lazyLibraryConnections.getObject(),
				filePropertyCache,
				new FilePropertiesProvider(
					lazyLibraryConnections.getObject(),
					filePropertyCache));
		}
	};

	private final CreateAndHold<SynchronizeStoredFiles> lazyStoredFilesSynchronization = new AbstractSynchronousLazy<SynchronizeStoredFiles>() {
		@Override
		protected SynchronizeStoredFiles create() {
			final StoredSyncService storedSyncService = StoredSyncService.this;

			final StoredItemAccess storedItemAccess = new StoredItemAccess(storedSyncService);

			final MediaQueryCursorProvider cursorProvider = new MediaQueryCursorProvider(
				storedSyncService,
				lazyFileProperties.getObject());

			final StoredFileUpdater storedFileUpdater = new StoredFileUpdater(
				storedSyncService,
				new MediaFileUriProvider(
					storedSyncService,
					cursorProvider,
					lazyReadPermissionArbitratorForOs.getObject(),
					lazyLibraryIdentifierProvider.getObject(),
					true),
				new MediaFileIdProvider(
					cursorProvider,
					lazyReadPermissionArbitratorForOs.getObject()),
				new StoredFileQuery(storedSyncService),
				lazyLibraryRepository.getObject(),
				lazyFileProperties.getObject(),
				new SyncDirectoryLookup(lazyLibraryRepository.getObject(), new PublicDirectoryLookup(storedSyncService), new PrivateDirectoryLookup(storedSyncService)));

			final LibrarySyncHandler syncHandler = new LibrarySyncHandler(
				new StoredItemServiceFileCollector(
					storedItemAccess,
					new LibraryFileProvider(new LibraryFileStringListProvider(lazyLibraryConnections.getObject())),
					FileListParameters.getInstance()),
				lazyStoredFileAccess.getObject(),
				storedFileUpdater,
				new StoredFileJobProcessor(
					new StoredFileSystemFileProducer(),
					lazyStoredFileAccess.getObject(),
					new StoredFileDownloader(new ServiceFileUriQueryParamsProvider(), lazyLibraryConnections.getObject()),
					new FileReadPossibleArbitrator(),
					new FileWritePossibleArbitrator(),
					new FileStreamWriter()));

			return new StoredFileSynchronization(
				lazyLibraryRepository.getObject(),
				lazyBroadcastManager.getObject(),
				syncHandler);
		}
	};

	private final CreateAndHold<ReceiveStoredFileEvent[]> lazyStoredFileEventReceivers = new AbstractSynchronousLazy<ReceiveStoredFileEvent[]>() {
		@Override
		protected ReceiveStoredFileEvent[] create() {
			final StoredSyncService storedSyncService = StoredSyncService.this;
			final StoredFileDownloadingNotifier storedFileDownloadingNotifier = new StoredFileDownloadingNotifier(
				lazyStoredFileAccess.getObject(),
				lazyFileProperties.getObject(),
				storedSyncService,
				storedSyncService);

			final StoredFileMediaScannerNotifier storedFileMediaScannerNotifier = new StoredFileMediaScannerNotifier(
				lazyStoredFileAccess.getObject(),
				new ScanMediaFileBroadcaster(StoredSyncService.this));

			final StoredFileReadPermissionsReceiver storedFileReadPermissionsReceiver = new StoredFileReadPermissionsReceiver(
				lazyReadPermissionArbitratorForOs.getObject(),
				new StorageReadPermissionsRequestedBroadcaster(lazyBroadcastManager.getObject()),
				lazyStoredFileAccess.getObject());

			final StoredFileWritePermissionsReceiver storedFileWritePermissionsReceiver = new StoredFileWritePermissionsReceiver(
				new ExternalStorageWritePermissionsArbitratorForOs(StoredSyncService.this),
				new StorageWritePermissionsRequestedBroadcaster(lazyBroadcastManager.getObject()),
				lazyStoredFileAccess.getObject());

			return new ReceiveStoredFileEvent[] {
				storedFileDownloadingNotifier,
				storedFileMediaScannerNotifier,
				storedFileReadPermissionsReceiver,
				storedFileWritePermissionsReceiver
			};
		}
	};

	private final CreateAndHold<SyncStartedReceiver> lazySyncStartedReceiver = new Lazy<>(() -> new SyncStartedReceiver(this));

	private final CreateAndHold<PowerManager.WakeLock> lazyWakeLock = new AbstractSynchronousLazy<PowerManager.WakeLock>() {
		@Override
		protected PowerManager.WakeLock create() {
			final PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
			return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService.class, "wakeLock"));
		}
	};

	private final CreateAndHold<PendingIntent> lazyShowDownloadsIntent = new Lazy<>(() -> PendingIntent.getActivity(
		this,
		0,
		browseLibraryIntent.getObject(),
		0));

	private final CreateAndHold<PendingIntent> lazyCancelIntent = new Lazy<>(() -> PendingIntent.getService(
		this,
		0,
		getSelfIntent(this, cancelSyncAction),
		PendingIntent.FLAG_UPDATE_CURRENT));

	private List<BroadcastReceiver> broadcastReceivers = new ArrayList<>();

	@Override
	public void onCreate() {
		super.onCreate();

		lazyWakeLock.getObject().acquire();
	}

	@Override
	public synchronized int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) return START_NOT_STICKY;

		final String action = intent.getAction();

		if (cancelSyncAction.equals(action)) {
			finish();
			return START_NOT_STICKY;
		}

		if (!doSyncAction.equals(action)) return START_NOT_STICKY;

		final boolean isUninterruptedSync = intent.getBooleanExtra(isUninterruptedSyncSetting, false);
		if (isSyncRunning()) {
			if (isUninterruptedSync) {
				if (onWifiStateChangedReceiver.isCreated())
					unregisterReceiver(onWifiStateChangedReceiver.getObject());
				if (onPowerDisconnectedReceiver.isCreated())
					unregisterReceiver(onPowerDisconnectedReceiver.getObject());
			}

			return START_NOT_STICKY;
		}

		if (!lazyStoredFileEventReceivers.isCreated()) {
			for (final ReceiveStoredFileEvent receiveStoredFileEvent : lazyStoredFileEventReceivers.getObject()) {
				final StoredFileBroadcastReceiver broadcastReceiver = new StoredFileBroadcastReceiver(receiveStoredFileEvent);
				if (!broadcastReceivers.add(broadcastReceiver)) continue;

				lazyBroadcastManager.getObject().registerReceiver(
					broadcastReceiver,
					Stream.of(receiveStoredFileEvent.acceptedEvents()).reduce(new IntentFilter(), (i, e) -> {
						i.addAction(e);
						return i;
					}));
			}

			lazyBroadcastManager.getObject().registerReceiver(
				lazySyncStartedReceiver.getObject(),
				Stream.of(lazySyncStartedReceiver.getObject().acceptedEvents()).reduce(new IntentFilter(), (i, e) -> {
					i.addAction(e);
					return i;
				}));
		}

		if (!isUninterruptedSync) {
			registerReceiver(onWifiStateChangedReceiver.getObject(), new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
			registerReceiver(onPowerDisconnectedReceiver.getObject(), new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
		}

		synchronizationDisposable = lazyStoredFilesSynchronization.getObject()
			.streamFileSynchronization()
			.subscribe(this::finish, this::finish);

		return START_NOT_STICKY;
	}

	private void finish(Throwable error) {
		logger.error("An error occurred while synchronizing stored files", error);
		finish();
	}

	private void finish() {
		lazySharedPreferences.getObject()
			.edit()
			.putLong(lastSyncTime, DateTime.now().getMillis())
			.apply();

		stopSelf();
	}

	@Override
	public void onDestroy() {
		if (synchronizationDisposable != null) {
			synchronizationDisposable.dispose();
			synchronizationDisposable = null;
		}

		if (lazyBroadcastManager.isCreated()) {
			while (!broadcastReceivers.isEmpty()) {
				final BroadcastReceiver receiver = broadcastReceivers.remove(0);
				lazyBroadcastManager.getObject().unregisterReceiver(receiver);
			}
		}

		if (onWifiStateChangedReceiver.isCreated())
			unregisterReceiver(onWifiStateChangedReceiver.getObject());

		if (onPowerDisconnectedReceiver.isCreated())
			unregisterReceiver(onPowerDisconnectedReceiver.getObject());

		if (lazyWakeLock.isCreated())
			lazyWakeLock.getObject().release();

		stopForeground(true);

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return lazyBinder.getObject();
	}

	private final Lazy<IBinder> lazyBinder = new Lazy<>(() -> new GenericBinder<>(this));

	@Override
	public void notify(String notificationText) {
		final NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this, lazyActiveNotificationChannelId.getObject());
		notifyBuilder
			.setSmallIcon(R.drawable.ic_stat_water_drop_white)
			.setContentIntent(lazyShowDownloadsIntent.getObject())
			.addAction(0, getString(R.string.stop_sync_button), lazyCancelIntent.getObject())
			.setOngoing(true)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setCategory(CATEGORY_PROGRESS);

		notifyBuilder.setContentTitle(getText(R.string.title_sync_files));
		if (notificationText != null)
			notifyBuilder.setContentText(notificationText);

		final Notification syncNotification = notifyBuilder.build();

		startForeground(notificationId, syncNotification);
	}
}
