package com.lasthopesoftware.bluewater.client.stored.library.items.files.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.fragment.adapter.ActiveFileDownloadsAdapter;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection;
import com.lasthopesoftware.bluewater.client.stored.worker.SyncWorker;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import java.util.Map;

public class ActiveFileDownloadsFragment extends Fragment {

	private BroadcastReceiver onSyncStartedReceiver;
	private BroadcastReceiver onSyncStoppedReceiver;
    private BroadcastReceiver onFileQueuedReceiver;
    private BroadcastReceiver onFileDownloadedReceiver;
    private LocalBroadcastManager localBroadcastManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	    localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
    }

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final RelativeLayout viewFileslayout = (RelativeLayout) inflater.inflate(R.layout.layout_downloads, container, false);

		final ProgressBar progressBar = viewFileslayout.findViewById(R.id.pbLoadingItems);
		final ListView listView = viewFileslayout.findViewById(R.id.lvItems);

		listView.setVisibility(View.INVISIBLE);
		progressBar.setVisibility(View.VISIBLE);

		final FragmentActivity activity = getActivity();
		if (activity == null) return viewFileslayout;

		final LibraryRepository libraryRepository = new LibraryRepository(activity);
		final SelectedBrowserLibraryProvider selectedBrowserLibraryProvider = new SelectedBrowserLibraryProvider(
			new SelectedBrowserLibraryIdentifierProvider(activity),
			libraryRepository);

		selectedBrowserLibraryProvider
			.getBrowserLibrary()
			.then(new VoidResponse<>(library -> {
				final StoredFileAccess storedFileAccess = new StoredFileAccess(
					activity,
					new StoredFilesCollection(activity));

				storedFileAccess.getDownloadingStoredFiles()
					.eventually(LoopedInPromise.response(new VoidResponse<>(storedFiles -> {
						final Map<Integer, StoredFile> localStoredFiles =
							Stream.of(storedFiles)
								.filter(f -> f.getLibraryId() == library.getId())
								.collect(Collectors.toMap(StoredFile::getId));

						final ActiveFileDownloadsAdapter activeFileDownloadsAdapter = new ActiveFileDownloadsAdapter(activity, localStoredFiles.values());

						if (onFileDownloadedReceiver != null)
							localBroadcastManager.unregisterReceiver(onFileDownloadedReceiver);

						onFileDownloadedReceiver = new BroadcastReceiver() {
							@Override
							public void onReceive(Context context, Intent intent) {
								final int storedFileId = intent.getIntExtra(SyncWorker.storedFileEventKey, -1);

								final StoredFile storedFile = localStoredFiles.get(storedFileId);
								if (storedFile == null) return;

								activeFileDownloadsAdapter.remove(new ServiceFile(storedFile.getServiceId()));
								localStoredFiles.remove(storedFileId);
							}
						};

						localBroadcastManager.registerReceiver(onFileDownloadedReceiver, new IntentFilter(SyncWorker.onFileDownloadedEvent));

						if (onFileQueuedReceiver != null)
							localBroadcastManager.unregisterReceiver(onFileQueuedReceiver);

						onFileQueuedReceiver = new BroadcastReceiver() {
							@Override
							public void onReceive(Context context, Intent intent) {
								final int storedFileId = intent.getIntExtra(SyncWorker.storedFileEventKey, -1);
								if (storedFileId == -1) return;

								if (localStoredFiles.containsKey(storedFileId)) return;

								storedFileAccess
									.getStoredFile(storedFileId)
									.eventually(LoopedInPromise.response(new VoidResponse<>(storedFile -> {
										if (storedFile == null || storedFile.getLibraryId() != library.getId())
											return;

										localStoredFiles.put(storedFileId, storedFile);
										activeFileDownloadsAdapter.add(new ServiceFile(storedFile.getServiceId()));
									}), activity));
							}
						};

						localBroadcastManager.registerReceiver(onFileQueuedReceiver, new IntentFilter(SyncWorker.onFileQueuedEvent));

						listView.setAdapter(activeFileDownloadsAdapter);

						progressBar.setVisibility(View.INVISIBLE);
						listView.setVisibility(View.VISIBLE);
					}), activity));
		}));

		final Button toggleSyncButton = viewFileslayout.findViewById(R.id.toggleSyncButton);
		final CharSequence startSyncLabel = activity.getText(R.string.start_sync_button);
		final CharSequence stopSyncLabel = activity.getText(R.string.stop_sync_button);

		SyncWorker.promiseIsSyncing()
			.eventually(LoopedInPromise.response(
				new VoidResponse<>(isSyncing -> toggleSyncButton.setText(!isSyncing ? startSyncLabel : stopSyncLabel)), activity));

		if (onSyncStartedReceiver != null)
			localBroadcastManager.unregisterReceiver(onSyncStartedReceiver);

		onSyncStartedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				toggleSyncButton.setText(stopSyncLabel);
			}
		};

		localBroadcastManager.registerReceiver(onSyncStartedReceiver, new IntentFilter(SyncWorker.onSyncStartEvent));

		if (onSyncStoppedReceiver != null)
			localBroadcastManager.unregisterReceiver(onSyncStoppedReceiver);

		onSyncStoppedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				toggleSyncButton.setText(startSyncLabel);
			}
		};

		localBroadcastManager.registerReceiver(onSyncStoppedReceiver, new IntentFilter(SyncWorker.onSyncStopEvent));

		toggleSyncButton.setOnClickListener(v -> SyncWorker.promiseIsSyncing()
			.then(isSyncing -> isSyncing
				? SyncWorker.cancel()
				: SyncWorker.syncImmediately(activity)));

		toggleSyncButton.setEnabled(true);

		return viewFileslayout;
	}

	@Override
    public void onDestroy() {
        super.onDestroy();

        if (localBroadcastManager == null) return;

		if (onSyncStartedReceiver != null)
			localBroadcastManager.unregisterReceiver(onSyncStartedReceiver);

		if (onSyncStoppedReceiver != null)
			localBroadcastManager.unregisterReceiver(onSyncStoppedReceiver);

        if (onFileDownloadedReceiver != null)
            localBroadcastManager.unregisterReceiver(onFileDownloadedReceiver);

        if (onFileQueuedReceiver != null)
            localBroadcastManager.unregisterReceiver(onFileQueuedReceiver);
    }
}
