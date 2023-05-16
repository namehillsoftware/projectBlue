package com.lasthopesoftware.bluewater.client.connection.polling

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.getIntent
import com.lasthopesoftware.bluewater.shared.android.intents.makePendingIntentImmutable
import com.lasthopesoftware.bluewater.shared.android.notifications.NoOpChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.SharedChannelProperties
import com.lasthopesoftware.bluewater.shared.android.services.GenericBinder
import com.lasthopesoftware.bluewater.shared.android.services.promiseBoundService
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise

class PollConnectionService : Service() {

	companion object {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<PollConnectionService>()) }

		private val stopWaitingForConnectionAction by lazy { magicPropertyBuilder.buildProperty("stopWaitingForConnection") }
		fun pollSessionConnection(context: Context, libraryId: LibraryId, withNotification: Boolean = false): Promise<IConnectionProvider> {
			return context.promiseBoundService<PollConnectionService>()
				.eventually {  s ->
					s.service
						.promiseTestedLibrary(libraryId, withNotification)
						.apply {
							must { context.unbindService(s.serviceConnection) }
						}
				}
		}
	}

	private var isNotifying = false

	private val notificationId = 99
	private val binder by lazy { GenericBinder(this) }

	private val channelConfiguration by lazy { SharedChannelProperties(this) }
	private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
	private val notificationsConfiguration by lazy {
		val notificationChannelActivator =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannelActivator(notificationManager)
			else NoOpChannelActivator()

		val channelName = notificationChannelActivator.activateChannel(channelConfiguration)
		NotificationsConfiguration(channelName, notificationId)
	}

	private val lazyNotificationController = lazy { NotificationsController(this, notificationManager) }

	private val messageBus by lazy { ApplicationMessageBus.getApplicationMessageBus() }

	private val libraryConnectionProvider by lazy { ConnectionSessionManager.get(this) }

	private val libraryConnectionPoller by lazy {
		LibraryConnectionPollingSessions(
			messageBus,
			LibraryConnectionPoller(libraryConnectionProvider),
		)
	}

	override fun onBind(intent: Intent): IBinder = binder

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (intent?.action == stopWaitingForConnectionAction)
			libraryConnectionPoller.cancelActiveConnections()

		return START_NOT_STICKY
	}

	private fun promiseTestedLibrary(libraryId: LibraryId, withNotification: Boolean): Promise<IConnectionProvider> {
		if (withNotification)
			beginNotification()

		return libraryConnectionPoller.pollConnection(libraryId)
	}

	private fun beginNotification() {
		if (isNotifying) return

		isNotifying = true

		// Add intent for canceling waiting for connection to come back
		val intent = getIntent<PollConnectionService>().apply {
			action = stopWaitingForConnectionAction
		}

		val pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT.makePendingIntentImmutable())

		val builder = NotificationCompat.Builder(this, notificationsConfiguration.notificationChannel)
			.setOngoing(true)
			.addAction(0, getText(R.string.btn_cancel), pi)
			.setContentTitle(getText(R.string.lbl_waiting_for_connection))
			.setContentText(getText(R.string.lbl_something_went_wrong))
			.setSmallIcon(R.drawable.now_playing_status_icon_white)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

		lazyNotificationController.value.notifyBackground(builder.build(), notificationsConfiguration.notificationId)
	}

	override fun onDestroy() {
		if (lazyNotificationController.isInitialized())
			lazyNotificationController.value.removeAllNotifications()
		super.onDestroy()
	}
}
