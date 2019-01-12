package com.lasthopesoftware.bluewater.client.stored.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import androidx.work.*;
import com.annimon.stream.Stream;
import com.google.common.util.concurrent.ListenableFuture;
import com.lasthopesoftware.bluewater.client.stored.worker.constraints.SyncWorkerConstraints;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.namehillsoftware.handoff.promises.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SyncSchedulingWorker extends Worker {

	private static final Logger logger = LoggerFactory.getLogger(SyncSchedulingWorker.class);

	private static final String workName = MagicPropertyBuilder.buildMagicPropertyName(SyncSchedulingWorker.class, "");

	public static Operation syncImmediately(Context context) {
		final OneTimeWorkRequest.Builder oneTimeWorkRequest = new OneTimeWorkRequest.Builder(SyncSchedulingWorker.class);
		oneTimeWorkRequest.setConstraints(constraints(context));
		return WorkManager.getInstance().enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, oneTimeWorkRequest.build());
	}

	public static Operation scheduleSync(Context context) {
		final PeriodicWorkRequest.Builder periodicWorkRequest = new PeriodicWorkRequest.Builder(SyncSchedulingWorker.class, 3, TimeUnit.HOURS);
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

	private final Context context;

	public SyncSchedulingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
		this.context = context;
	}

	@NonNull
	@Override
	public Result doWork() {
		return Result.success();
	}

	@Override
	public void onStopped() {}
}
