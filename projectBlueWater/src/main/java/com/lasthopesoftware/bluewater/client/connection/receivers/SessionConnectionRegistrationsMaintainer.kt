package com.lasthopesoftware.bluewater.client.connection.receivers

import android.content.Context
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class SessionConnectionRegistrationsMaintainer(
	private val context: Context,
	private val applicationMessages: RegisterForApplicationMessages,
	private val connectionDependentReceiverRegistrations: Collection<RegisterReceiverForEvents>
) :
	(SelectedConnection.BuildSessionConnectionBroadcast) -> Unit,
	AutoCloseable,
	ImmediateResponse<List<(ApplicationMessage) -> Unit>, Unit>
{
	private var registrationPromise = Promise(emptyList<(ApplicationMessage) -> Unit>())

	@Synchronized
	override fun invoke(broadcast: SelectedConnection.BuildSessionConnectionBroadcast) {
		if (broadcast.buildingConnectionStatus != BuildingConnectionStatus.BuildingConnectionComplete) return

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
