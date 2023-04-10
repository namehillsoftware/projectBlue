package com.lasthopesoftware.bluewater.client.connection.polling

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.getIntent
import com.lasthopesoftware.bluewater.shared.android.intents.safelyGetParcelableExtra
import com.lasthopesoftware.bluewater.shared.android.makePendingIntentImmutable
import com.lasthopesoftware.bluewater.shared.android.notifications.NoOpChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.SharedChannelProperties
import com.lasthopesoftware.bluewater.shared.android.services.GenericBinder
import com.lasthopesoftware.bluewater.shared.android.services.promiseBoundService
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.resources.closables.AutoCloseableManager
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken
import java.util.concurrent.CancellationException

class PollConnectionService : Service(), MessengerOperator<IConnectionProvider> {

	companion object {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<PollConnectionService>()) }

		private val stopWaitingForConnectionAction by lazy { magicPropertyBuilder.buildProperty("stopWaitingForConnection") }

		private val libraryIdProperty by lazy  { magicPropertyBuilder.buildProperty("libraryId") }

		fun pollSessionConnection(context: Context, libraryId: LibraryId, withNotification: Boolean = false): Promise<IConnectionProvider> {
			val bundle = Bundle().apply {
				putParcelable(libraryIdProperty, libraryId)
			}

			return context.promiseBoundService<PollConnectionService>(bundle)
				.eventually {  s ->
					s.service.run {
						this.withNotification = this.withNotification || withNotification
						if (libraryIdUnderTest != libraryId) Promise(IllegalStateException("libraryId is not the libraryIdUnderTest."))
						else lazyConnectionPoller.value.must { context.unbindService(s.serviceConnection) }
					}
				}
		}
	}

	class ConnectionLostNotification(val libraryId: LibraryId) : ApplicationMessage

	private var withNotification = false

	private val notificationId = 99
	private val binder by lazy { GenericBinder(this) }
	private val handler by lazy { Handler(mainLooper) }

	private val lazyCloseableManager = lazy { AutoCloseableManager() }
	private val messageBus by lazy {
		getApplicationMessageBus().getScopedMessageBus().also(lazyCloseableManager.value::manage)
	}

	private val lazyConnectionPoller = lazy {
		messageBus.sendMessage(ConnectionLostNotification(libraryIdUnderTest))
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

	private val libraryConnectionProvider by lazy { ConnectionSessionManager.get(this) }

	private lateinit var libraryIdUnderTest: LibraryId

	override fun onBind(intent: Intent): IBinder = binder

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (intent?.action == stopWaitingForConnectionAction && lazyConnectionPoller.isInitialized())
			lazyConnectionPoller.value.cancel()

		if (!this::libraryIdUnderTest.isInitialized) {
			val libraryId = intent?.safelyGetParcelableExtra<LibraryId>(libraryIdProperty)
			if (libraryId != null) {
				libraryIdUnderTest = libraryId
			}
		}

		return START_NOT_STICKY
	}

	override fun send(messenger: Messenger<IConnectionProvider>) {
		val cancellationToken = CancellationToken()
		messenger.cancellationRequested(cancellationToken)

		lazyCloseableManager.value.manage(messageBus.registerReceiver { m: BrowserLibrarySelection.LibraryChosenMessage ->
			if (m.chosenLibraryId != libraryIdUnderTest)
				cancellationToken.run()
		})

		pollSessionConnection(messenger, cancellationToken, 1000)
	}

	private fun pollSessionConnection(messenger: Messenger<IConnectionProvider>, cancellationToken: CancellationToken, connectionTime: Int) {
		if (cancellationToken.isCancelled) {
			messenger.sendRejection(CancellationException("Polling the session connection was cancelled"))
			return
		}

		if (withNotification) beginNotification()

		val nextConnectionTime = if (connectionTime < 32000) connectionTime * 2 else connectionTime
		libraryConnectionProvider
			.promiseTestedLibraryConnection(libraryIdUnderTest)
			.then({
				if (it == null) {
					handler.postDelayed(
						{ pollSessionConnection(messenger, cancellationToken, nextConnectionTime) },
						connectionTime.toLong())
				} else {
					messenger.sendResolution(it)
				}
			}, {
				handler.postDelayed(
					{ pollSessionConnection(messenger, cancellationToken, nextConnectionTime) },
					connectionTime.toLong())
			})
	}

	private fun beginNotification() {
		// Add intent for canceling waiting for connection to come back
		val intent = getIntent<PollConnectionService>()
		intent.action = stopWaitingForConnectionAction

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
		if (lazyCloseableManager.isInitialized())
			lazyCloseableManager.value.close()
		super.onDestroy()
	}
}
