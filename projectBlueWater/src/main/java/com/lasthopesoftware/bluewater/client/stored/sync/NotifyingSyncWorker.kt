package com.lasthopesoftware.bluewater.client.stored.sync

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.stored.sync.constraints.SyncWorkerConstraints
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.SyncChannelProperties
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.notifications.NoOpChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.bluewater.shared.makePendingIntentImmutable
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class NotifyingSyncWorker(private val context: Context, workerParams: WorkerParameters)
	: SyncWorker(context, workerParams)
{
	companion object {
		private const val notificationId = 23
		private const val workName = "StoredFilesSync"

		fun syncImmediately(context: Context): Promise<Operation> {
			val oneTimeWorkRequest = OneTimeWorkRequest.Builder(NotifyingSyncWorker::class.java)
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
				val periodicWorkRequest = PeriodicWorkRequest.Builder(NotifyingSyncWorker::class.java, 3, TimeUnit.HOURS)
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

	private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
	private val channelConfiguration by lazy { SyncChannelProperties(context) }

	private val activeNotificationChannelId by lazy {
		val notificationChannelActivator =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannelActivator(notificationManager)
			else NoOpChannelActivator()
		notificationChannelActivator.activateChannel(channelConfiguration)
	}

	private val browseLibraryIntent by lazy {
		val browseLibraryIntent = Intent(context, BrowserEntryActivity::class.java)
		browseLibraryIntent.action = BrowserEntryActivity.showDownloadsAction
		browseLibraryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
	}

	private val showDownloadsIntent by lazy {
		PendingIntent.getActivity(
			context,
			0,
			browseLibraryIntent,
			0.makePendingIntentImmutable())
	}

	private val cancelIntent by lazy { WorkManager.getInstance(context).createCancelPendingIntent(id) }

	private val promisedNotifications = ConcurrentHashMap<Promise<Unit>, Unit>()

	override fun continueWork(cancellationProxy: CancellationProxy): Promise<Unit> =
		Promise.whenAll(promisedNotifications.keys).unitResponse()

	override fun notify(notificationText: String?) {
		val notifyBuilder = NotificationCompat.Builder(context, activeNotificationChannelId)
		notifyBuilder
			.setSmallIcon(R.drawable.ic_stat_water_drop_white)
			.setContentIntent(showDownloadsIntent)
			.addAction(0, context.getString(R.string.stop_sync_button), cancelIntent)
			.setOngoing(true)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
		notifyBuilder.setContentTitle(context.getText(R.string.title_sync_files))
		notifyBuilder.setContentText(notificationText)
		val syncNotification = notifyBuilder.build()
		val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
		val promisedForegroundNotification = setForegroundAsync(ForegroundInfo(notificationId, syncNotification, serviceType))
			.toPromise(ThreadPools.compute)
			.unitResponse()
		promisedNotifications.putIfAbsent(promisedForegroundNotification, Unit)
		promisedForegroundNotification.must { promisedNotifications.remove(promisedForegroundNotification) }
	}

}
