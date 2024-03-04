package com.lasthopesoftware.bluewater.client.stored.sync

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.lasthopesoftware.bluewater.client.stored.sync.constraints.SyncWorkerConstraints
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.TimeUnit

private const val workName = "StoredFilesSync"

class SyncScheduler(private val context: Context) : ScheduleSyncs {

	override fun syncImmediately(): Promise<Operation> {
		val oneTimeWorkRequest = OneTimeWorkRequest.Builder(SyncWorker::class.java).build()

		return WorkManager.getInstance(context)
			.beginUniqueWork(workName, ExistingWorkPolicy.REPLACE, oneTimeWorkRequest)
			.enqueue()
			.result
			.toPromise(ThreadPools.compute)
			.eventually(
				{ scheduleSync() },
				{ scheduleSync() })
	}

	override fun scheduleSync(): Promise<Operation> =
		constraints().then { c ->
			val workerClass =
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) SyncWorker::class.java
				else MutedSyncWorker::class.java
			val periodicWorkRequest = PeriodicWorkRequest.Builder(workerClass, 3, TimeUnit.HOURS)
			periodicWorkRequest.setConstraints(c)
			WorkManager.getInstance(context)
				.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest.build())
		}

	override fun cancelSync(): Promise<Unit> =
		promiseWorkInfos().then { wi ->
			val workManager = WorkManager.getInstance(context)
			for (item in wi.filter { it.state == WorkInfo.State.RUNNING })
				workManager.cancelWorkById(item.id)
		}

	override fun constraints(): Promise<Constraints> {
		val applicationSettings = context.getApplicationSettingsRepository()
		return SyncWorkerConstraints(applicationSettings).currentConstraints
	}

	override fun promiseIsSyncing(): Promise<Boolean> =
		promiseWorkInfos().then { workInfos -> workInfos.any { wi -> wi.state == WorkInfo.State.RUNNING } }

	override fun promiseIsScheduled(): Promise<Boolean> =
		promiseWorkInfos().then { workInfos -> workInfos.any { wi -> wi.state == WorkInfo.State.ENQUEUED } }

	override fun promiseWorkInfos(): Promise<List<WorkInfo>> =
		WorkManager.getInstance(context).getWorkInfosForUniqueWork(workName).toPromise(ThreadPools.compute)
}
