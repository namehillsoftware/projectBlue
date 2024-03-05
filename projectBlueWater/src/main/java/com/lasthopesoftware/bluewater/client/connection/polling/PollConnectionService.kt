package com.lasthopesoftware.bluewater.client.connection.polling

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
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
import com.lasthopesoftware.resources.closables.lazyScoped
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.ProxyPromise

class PollConnectionService : LifecycleService() {

	companion object {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<PollConnectionService>()) }

		private val stopWaitingForConnectionAction by lazy { magicPropertyBuilder.buildProperty("stopWaitingForConnection") }
		fun pollSessionConnection(context: Context, libraryId: LibraryId): Promise<IConnectionProvider> = ProxyPromise { cp ->
			context.promiseBoundService<PollConnectionService>()
				.also(cp::doCancel)
				.eventually {  s ->
					s.service
						.promiseTestedLibrary(libraryId)
						.also(cp::doCancel)
						.must { _ -> s.close() }
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
			else NoOpChannelActivator

		val channelName = notificationChannelActivator.activateChannel(channelConfiguration)
		NotificationsConfiguration(channelName, notificationId)
	}

	private val notificationController by lazyScoped { NotificationsController(this, notificationManager) }

	private val libraryConnectionProvider by lazy { ConnectionSessionManager.get(this) }

	private val libraryConnectionPoller by lazy {
		LibraryConnectionPollingSessions(
			LibraryConnectionPoller(libraryConnectionProvider),
		)
	}

	override fun onBind(intent: Intent): IBinder {
		super.onBind(intent)
		return binder
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)

		if (intent?.action == stopWaitingForConnectionAction)
			libraryConnectionPoller.cancelActiveConnections()

		return START_NOT_STICKY
	}

	private fun promiseTestedLibrary(libraryId: LibraryId): Promise<IConnectionProvider> {
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

		notificationController.notifyBackground(builder.build(), notificationsConfiguration.notificationId)
	}
}
