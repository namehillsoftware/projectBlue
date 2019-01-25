package com.lasthopesoftware.bluewater.client.stored.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import androidx.work.*;
import com.lasthopesoftware.bluewater.client.stored.service.StoredSyncService;
import com.lasthopesoftware.bluewater.client.stored.worker.constraints.SyncWorkerConstraints;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SyncSchedulingWorker extends Worker {

	private static final Logger logger = LoggerFactory.getLogger(SyncSchedulingWorker.class);

	private static final String workName = MagicPropertyBuilder.buildMagicPropertyName(SyncSchedulingWorker.class, "");

	public static Operation scheduleSync(Context context) {
		final PeriodicWorkRequest.Builder periodicWorkRequest = new PeriodicWorkRequest.Builder(SyncSchedulingWorker.class, 3, TimeUnit.HOURS);
		periodicWorkRequest.setConstraints(constraints(context));
		return WorkManager.getInstance()
			.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest.build());
	}

	private static Constraints constraints(Context context) {
		final SharedPreferences manager = PreferenceManager.getDefaultSharedPreferences(context);
		return new SyncWorkerConstraints(manager).getCurrentConstraints();
	}

	public static Operation cancel() {
		return WorkManager.getInstance().cancelUniqueWork(workName);
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
