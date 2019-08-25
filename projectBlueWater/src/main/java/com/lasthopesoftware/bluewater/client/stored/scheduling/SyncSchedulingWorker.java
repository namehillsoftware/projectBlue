package com.lasthopesoftware.bluewater.client.stored.scheduling;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import androidx.work.*;
import com.annimon.stream.Stream;
import com.google.common.util.concurrent.ListenableFuture;
import com.lasthopesoftware.bluewater.client.stored.scheduling.constraints.SyncWorkerConstraints;
import com.lasthopesoftware.bluewater.client.stored.service.StoredSyncService;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SyncSchedulingWorker extends Worker {

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

	private final Context context;

	public SyncSchedulingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
		this.context = context;
	}

	@NonNull
	@Override
	public Result doWork() {
		StoredSyncService.doSync(context);
		return Result.success();
	}

	@Override
	public void onStopped() {}
}
