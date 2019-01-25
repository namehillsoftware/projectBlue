package com.lasthopesoftware.bluewater.client.stored.service.adapter;

import android.accounts.Account;
import android.content.*;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.io.FileStreamWriter;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection;
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup;
import com.lasthopesoftware.bluewater.client.stored.library.sync.factory.LibrarySyncHandlerFactory;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;
import com.lasthopesoftware.bluewater.client.stored.sync.SynchronizeStoredFiles;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.lasthopesoftware.storage.directories.PrivateDirectoryLookup;
import com.lasthopesoftware.storage.directories.PublicDirectoryLookup;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private final AbstractSynchronousLazy<BroadcastReceiver> onWifiStateChangedReceiver = new AbstractSynchronousLazy<BroadcastReceiver>() {
		@Override
		protected final BroadcastReceiver create() {
			return new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
//					if (!IoCommon.isWifiConnected(context)) cancelSync(StoredSyncService.this);
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

				}
			};
		}
	};

	private final CreateAndHold<SynchronizeStoredFiles> lazyStoredFilesSynchronization = new AbstractSynchronousLazy<SynchronizeStoredFiles>() {
		@Override
		protected SynchronizeStoredFiles create() {
			final OkHttpClient client = new OkHttpClient.Builder()
				.connectTimeout(10, TimeUnit.SECONDS)
				.build();

			final UrlScanner urlScanner = new UrlScanner(
				new ConnectionTester(),
				new ServerLookup(new ServerInfoXmlRequest(client)),
				OkHttpFactory.getInstance());

			final StoredFileAccess storedFileAccess = new StoredFileAccess(getContext(), new StoredFilesCollection(getContext()));

			return new StoredFileSynchronization(
				new LibraryRepository(getContext()),
				LocalBroadcastManager.getInstance(getContext()),
				urlScanner,
				new LibrarySyncHandlerFactory(
					storedFileAccess,
					getContext(),
					new ExternalStorageReadPermissionsArbitratorForOs(getContext()),
					new SyncDirectoryLookup(new PublicDirectoryLookup(getContext()), new PrivateDirectoryLookup(getContext())),
					new StoredFileSystemFileProducer(),
					new ServiceFileUriQueryParamsProvider(),
					new FileReadPossibleArbitrator(),
					new FileWritePossibleArbitrator(),
					new FileStreamWriter())
			);
		}
	};

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
	}

	public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		if (isDeviceStateValidForSync())
			lazyStoredFilesSynchronization.getObject().streamFileSynchronization().blockingAwait();
	}

	private boolean isDeviceStateValidForSync() {
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

		final boolean isSyncOnWifiOnly = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnWifiOnlyKey, false);
		if (isSyncOnWifiOnly) {
			if (!IoCommon.isWifiConnected(getContext())) return false;

			getContext().registerReceiver(onWifiStateChangedReceiver.getObject(), new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		}

		final boolean isSyncOnPowerOnly = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, false);
		if (isSyncOnPowerOnly) {
			if (!IoCommon.isPowerConnected(getContext())) return false;

			getContext().registerReceiver(onPowerDisconnectedReceiver.getObject(), new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
		}

		return true;
	}
}
