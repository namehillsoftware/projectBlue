package com.lasthopesoftware.bluewater.client.stored.sync

import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.lasthopesoftware.bluewater.client.stored.sync.constraints.ConstrainSyncWork
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.TimeUnit

private const val workName = "StoredFilesSync"

class SyncScheduler(private val workManager: WorkManager, private val syncWorkerConstraints: ConstrainSyncWork) : ScheduleSyncs {

	override fun syncImmediately(): Promise<Operation> {
		val oneTimeWorkRequest = OneTimeWorkRequest.Builder(SyncWorker::class.java).build()

		return workManager
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
			workManager
				.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest.build())
		}

	override fun cancelSync(): Promise<Unit> =
		promiseWorkInfos().then { wi ->
			for (item in wi.filter { it.state == WorkInfo.State.RUNNING })
				workManager.cancelWorkById(item.id)
		}

	override fun constraints(): Promise<Constraints> = syncWorkerConstraints.currentConstraints

	override fun promiseIsSyncing(): Promise<Boolean> =
		promiseWorkInfos().then { workInfos -> workInfos.any { wi -> wi.state == WorkInfo.State.RUNNING } }

	override fun promiseIsScheduled(): Promise<Boolean> =
		promiseWorkInfos().then { workInfos -> workInfos.any { wi -> wi.state == WorkInfo.State.ENQUEUED } }

	override fun promiseWorkInfos(): Promise<List<WorkInfo>> =
		workManager.getWorkInfosForUniqueWork(workName).toPromise(ThreadPools.compute)
}
