package com.lasthopesoftware.bluewater.client.connection.receivers

import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class SessionConnectionRegistrationsMaintainer(
	private val context: Context,
	private val applicationMessages: RegisterForApplicationMessages,
	private val connectionDependentReceiverRegistrations: Collection<RegisterReceiverForEvents>
) :
	ReceiveBroadcastEvents,
	AutoCloseable,
	ImmediateResponse<List<(ApplicationMessage) -> Unit>, Unit>
{
	private var registrationPromise = Promise(emptyList<(ApplicationMessage) -> Unit>())

	@Synchronized
	override fun onReceive(intent: Intent) {
		val buildSessionStatus = intent.getIntExtra(SelectedConnection.buildSessionBroadcastStatus, -1)
		if (buildSessionStatus != SelectedConnection.BuildingSessionConnectionStatus.BuildingSessionComplete) return

		registrationPromise = registrationPromise
			.then(this)
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

	override fun close() {
		registrationPromise.then(this)
	}

	override fun respond(registeredReceivers: List<(ApplicationMessage) -> Unit>) {
		for (receiver in registeredReceivers)
			applicationMessages.unregisterReceiver(receiver)
	}
}
