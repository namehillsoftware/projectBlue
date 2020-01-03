package com.lasthopesoftware.bluewater.client.stored.scheduling;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.annimon.stream.Stream;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.LibraryFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.LibraryFileStringListProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.stored.library.sync.CheckForSync;
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncChecker;
import com.lasthopesoftware.bluewater.client.stored.scheduling.constraints.SyncWorkerConstraints;
import com.lasthopesoftware.bluewater.client.stored.service.StoredSyncService;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SyncSchedulingWorker extends ListenableWorker {

	private static final String workName = MagicPropertyBuilder.buildMagicPropertyName(SyncSchedulingWorker.class, "");

	public static Operation scheduleSync(Context context) {
		final PeriodicWorkRequest.Builder periodicWorkRequest = new PeriodicWorkRequest.Builder(SyncSchedulingWorker.class, 3, TimeUnit.HOURS);
		periodicWorkRequest.setConstraints(constraints(context));
		return WorkManager.getInstance(context)
			.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest.build());
	}

	private static Constraints constraints(Context context) {
		final SharedPreferences manager = PreferenceManager.getDefaultSharedPreferences(context);
		return new SyncWorkerConstraints(manager).getCurrentConstraints();
	}

	public static Promise<Boolean> promiseIsScheduled(Context context) {
		return promiseWorkInfos(context)
			.then(workInfos -> Stream.of(workInfos).anyMatch(wi -> wi.getState() == WorkInfo.State.ENQUEUED));
	}

	private static Promise<List<WorkInfo>> promiseWorkInfos(Context context) {
		return new Promise<>(m -> {
			final ListenableFuture<List<WorkInfo>> workInfosByName = WorkManager.getInstance(context).getWorkInfosForUniqueWork(workName);
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

	private final CreateAndHold<CheckForSync> lazySyncChecker = new AbstractSynchronousLazy<CheckForSync>() {
		@Override
		protected CheckForSync create() {
			return new SyncChecker(
				new LibraryRepository(context),
				new StoredItemServiceFileCollector(
					new StoredItemAccess(context),
					new LibraryFileProvider(new LibraryFileStringListProvider(LibraryConnectionProvider.Instance.get(context))),
					FileListParameters.getInstance()));
		}
	};

	private final Context context;

	public SyncSchedulingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
		this.context = context;
	}

	@NonNull
	@Override
	public ListenableFuture<Result> startWork() {
		final SettableFuture<Result> futureResult = SettableFuture.create();
		lazySyncChecker.getObject().promiseIsSyncNeeded()
			.then(new VoidResponse<>(isNeeded -> {
				if (isNeeded)
					StoredSyncService.doSync(context);
			}))
			.then(
				v -> futureResult.set(Result.success()),
				futureResult::setException);
		return futureResult;
	}

	@Override
	public void onStopped() {}
}
