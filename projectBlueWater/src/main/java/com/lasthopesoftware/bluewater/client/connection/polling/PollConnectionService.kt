package com.lasthopesoftware.bluewater.client.connection.polling

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.makePendingIntentImmutable
import com.lasthopesoftware.bluewater.shared.android.notifications.NoOpChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.SharedChannelProperties
import com.lasthopesoftware.bluewater.shared.android.services.GenericBinder
import com.lasthopesoftware.bluewater.shared.android.services.promiseBoundService
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken
import java.util.concurrent.CancellationException

class PollConnectionService : Service(), MessengerOperator<IConnectionProvider> {

	companion object {
		fun pollSessionConnection(context: Context, withNotification: Boolean = false): Promise<IConnectionProvider> =
			context.promiseBoundService<PollConnectionService>()
				.eventually {  s ->
					s.service.let {
						it.withNotification = it.withNotification || withNotification
						it.lazyConnectionPoller.value.must { context.unbindService(s.serviceConnection) }
					}
				}

		private val stopWaitingForConnectionAction by lazy { MagicPropertyBuilder.buildMagicPropertyName<PollConnectionService>("stopWaitingForConnection") }
	}

	object ConnectionLostNotification : ApplicationMessage

	private var withNotification = false

	private val notificationId = 99
	private val binder by lazy { GenericBinder(this) }
	private val handler by lazy { Handler(mainLooper) }

	private val lazyConnectionPoller = lazy {
		getApplicationMessageBus().sendMessage(ConnectionLostNotification)
		Promise(this)
	}

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

	override fun onBind(intent: Intent): IBinder = binder

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (intent?.action == stopWaitingForConnectionAction && lazyConnectionPoller.isInitialized())
			lazyConnectionPoller.value.cancel()

		return START_NOT_STICKY
	}

	override fun send(messenger: Messenger<IConnectionProvider>) {
		val cancellationToken = CancellationToken()
		messenger.cancellationRequested(cancellationToken)
		pollSessionConnection(messenger, cancellationToken, 1000)
	}

	private fun pollSessionConnection(messenger: Messenger<IConnectionProvider>, cancellationToken: CancellationToken, connectionTime: Int) {
		if (cancellationToken.isCancelled) {
			messenger.sendRejection(CancellationException("Polling the session connection was cancelled"))
			return
		}

		if (withNotification) beginNotification()

		val nextConnectionTime = if (connectionTime < 32000) connectionTime * 2 else connectionTime
		getInstance(this)
			.promiseTestedSessionConnection()
			.then({
				when (it) {
					null -> handler.postDelayed(
						{ pollSessionConnection(messenger, cancellationToken, nextConnectionTime) },
						connectionTime.toLong())
					else -> messenger.sendResolution(it)
				}
			}, {
				handler.postDelayed(
					{ pollSessionConnection(messenger, cancellationToken, nextConnectionTime) },
					connectionTime.toLong())
			})
	}

	private fun beginNotification() {
		// Add intent for canceling waiting for connection to come back
		val intent = Intent(this, PollConnectionService::class.java)
		intent.action = stopWaitingForConnectionAction

		val pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT.makePendingIntentImmutable())

		val builder = NotificationCompat.Builder(this, notificationsConfiguration.notificationChannel)
			.setOngoing(true)
			.setContentIntent(pi)
			.setContentTitle(getText(R.string.lbl_waiting_for_connection))
			.setContentText(getText(R.string.lbl_click_to_cancel))
			.setSmallIcon(R.drawable.launcher_icon_dark)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

		lazyNotificationController.value.notifyBackground(builder.build(), notificationsConfiguration.notificationId)
	}

	override fun onDestroy() {
		if (lazyNotificationController.isInitialized())
			lazyNotificationController.value.removeAllNotifications()
		super.onDestroy()
	}
}
