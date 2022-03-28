package com.lasthopesoftware.bluewater.client.connection.receivers

import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class SessionConnectionRegistrationsMaintainer(
	private val context: Context,
	private val messageRegistrar: RegisterForMessages,
	private val applicationMessages: RegisterForApplicationMessages,
	private val connectionDependentReceiverRegistrations: Collection<RegisterReceiverForEvents>
) :
	ReceiveBroadcastEvents,
	AutoCloseable,
	ImmediateResponse<List<ReceiveBroadcastEvents>, Unit>,
	(ApplicationMessage) -> Unit
{
	private var broadcastRegistrationPromise = Promise(emptyList<ReceiveBroadcastEvents>())
	private var registrationPromise = Promise(emptyList<(ApplicationMessage) -> Unit>())

	@Synchronized
	override fun onReceive(intent: Intent) {
		val buildSessionStatus = intent.getIntExtra(SelectedConnection.buildSessionBroadcastStatus, -1)
		if (buildSessionStatus != SelectedConnection.BuildingSessionConnectionStatus.BuildingSessionComplete) return

		broadcastRegistrationPromise = broadcastRegistrationPromise
			.then(this) // remove existing registrations
			.eventually { getInstance(context).promiseSessionConnection() }
			.then { connectionProvider ->
				connectionProvider ?: return@then emptyList()
				connectionDependentReceiverRegistrations.map { registration ->
					val receiver = registration.registerBroadcastEventsWithConnectionProvider(connectionProvider)
					for (i in registration.forIntents())
						messageRegistrar.registerReceiver(receiver, i)

					receiver
				}
			}

		registrationPromise = registrationPromise
			.then {
				for (receiver in it)
					applicationMessages.unregisterReceiver(receiver)
			}
			.eventually { getInstance(context).promiseSessionConnection() }
			.then { connectionProvider ->
				connectionProvider ?: return@then emptyList()
				connectionDependentReceiverRegistrations.map { registration ->
					val receiver = registration.registerWithConnectionProvider(connectionProvider)
					for (c in registration.forClasses())
						applicationMessages.registerForClass(c, receiver)

					receiver
				}
			}
	}

	override fun invoke(message: ApplicationMessage) {
		TODO("Not yet implemented")
	}

	override fun close() {
		broadcastRegistrationPromise.then(this)
	}

	override fun respond(registeredReceivers: List<ReceiveBroadcastEvents>) {
		for (receiver in registeredReceivers)
			messageRegistrar.unregisterReceiver(receiver)
	}
}
