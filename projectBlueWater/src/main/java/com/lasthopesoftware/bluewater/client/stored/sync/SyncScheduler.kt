package com.lasthopesoftware.bluewater.client.stored.sync

import android.content.Context
import android.os.Build
import androidx.work.*
import com.lasthopesoftware.bluewater.client.stored.sync.constraints.SyncWorkerConstraints
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.TimeUnit

object SyncScheduler {
	private const val workName = "StoredFilesSync"

	fun syncImmediately(context: Context): Promise<Operation> {
		val oneTimeWorkRequest = OneTimeWorkRequest.Builder(SyncWorker::class.java)
		return WorkManager.getInstance(context)
			.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, oneTimeWorkRequest.build())
			.result
			.toPromise(ThreadPools.compute)
			.eventually(
				{ scheduleSync(context) },
				{ scheduleSync(context) })
	}

	fun scheduleSync(context: Context): Promise<Operation> =
		constraints(context).then { c ->
			val workerClass =
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) SyncWorker::class.java
				else MutedSyncWorker::class.java
			val periodicWorkRequest = PeriodicWorkRequest.Builder(workerClass, 3, TimeUnit.HOURS)
			periodicWorkRequest.setConstraints(c)
			WorkManager.getInstance(context)
				.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest.build())
		}

	fun cancelSync(context: Context): Promise<Unit> =
		promiseWorkInfos(context).then { wi ->
			val workManager = WorkManager.getInstance(context)
			for (item in wi.filter { it.state == WorkInfo.State.RUNNING })
				workManager.cancelWorkById(item.id)
		}

	private fun constraints(context: Context): Promise<Constraints> {
		val applicationSettings = context.getApplicationSettingsRepository()
		return SyncWorkerConstraints(applicationSettings).currentConstraints
	}

	fun promiseIsSyncing(context: Context): Promise<Boolean> =
		promiseWorkInfos(context).then { workInfos -> workInfos.any { wi -> wi.state == WorkInfo.State.RUNNING } }

	fun promiseIsScheduled(context: Context): Promise<Boolean> =
		promiseWorkInfos(context).then { workInfos -> workInfos.any { wi -> wi.state == WorkInfo.State.ENQUEUED } }

	private fun promiseWorkInfos(context: Context): Promise<List<WorkInfo>> =
		WorkManager.getInstance(context).getWorkInfosForUniqueWork(workName).toPromise(ThreadPools.compute)
}
