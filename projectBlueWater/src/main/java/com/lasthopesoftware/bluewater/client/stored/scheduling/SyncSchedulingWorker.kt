package com.lasthopesoftware.bluewater.client.stored.scheduling

import android.content.Context
import android.os.AsyncTask
import android.preference.PreferenceManager
import androidx.work.*
import com.annimon.stream.Stream
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider.Instance.get
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.sync.CheckForSync
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncChecker
import com.lasthopesoftware.bluewater.client.stored.scheduling.constraints.SyncWorkerConstraints
import com.lasthopesoftware.bluewater.client.stored.service.StoredSyncService
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.lazyj.CreateAndHold
import com.namehillsoftware.lazyj.Lazy
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class SyncSchedulingWorker(private val context: Context, workerParams: WorkerParameters) : ListenableWorker(context, workerParams) {
	private val lazySyncChecker: CreateAndHold<CheckForSync> = Lazy<CheckForSync> {
			SyncChecker(
				LibraryRepository(context),
				StoredItemServiceFileCollector(
					StoredItemAccess(context),
					LibraryFileProvider(LibraryFileStringListProvider(get(context))),
					FileListParameters.getInstance()))
	}

	override fun startWork(): ListenableFuture<Result> {
		val futureResult = SettableFuture.create<Result>()
		lazySyncChecker.getObject().promiseIsSyncNeeded()
			.then { isNeeded ->
				if (isNeeded) StoredSyncService.doSync(context)
				futureResult.set(Result.success())
			}
			.excuse { futureResult.setException(it) }
		return futureResult
	}

	override fun onStopped() {}

	companion object {
		private val workName = MagicPropertyBuilder.buildMagicPropertyName(SyncSchedulingWorker::class.java, "")
		@JvmStatic
		fun scheduleSync(context: Context): Operation {
			val periodicWorkRequest = PeriodicWorkRequest.Builder(SyncSchedulingWorker::class.java, 3, TimeUnit.HOURS)
			periodicWorkRequest.setConstraints(constraints(context))
			return WorkManager.getInstance(context)
				.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest.build())
		}

		private fun constraints(context: Context): Constraints {
			val manager = PreferenceManager.getDefaultSharedPreferences(context)
			return SyncWorkerConstraints(manager).currentConstraints
		}

		@JvmStatic
		fun promiseIsScheduled(context: Context): Promise<Boolean> {
			return promiseWorkInfos(context)
				.then { workInfos -> Stream.of(workInfos).anyMatch { wi -> wi.state == WorkInfo.State.ENQUEUED } }
		}

		private fun promiseWorkInfos(context: Context): Promise<List<WorkInfo>> {
			return object : Promise<List<WorkInfo>>() {
				init {
					val workInfosByName = WorkManager.getInstance(context).getWorkInfosForUniqueWork(workName)
					respondToCancellation { workInfosByName.cancel(false) }
					workInfosByName.addListener(Runnable {
						try {
							resolve(workInfosByName.get())
						} catch (e: ExecutionException) {
							reject(e)
						} catch (e: InterruptedException) {
							reject(e)
						}
					}, AsyncTask.THREAD_POOL_EXECUTOR)
				}
			}
		}
	}
}
