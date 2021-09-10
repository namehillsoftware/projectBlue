package com.lasthopesoftware.bluewater.client.connection.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

/**
 * Created by david on 3/19/17.
 */
class SessionConnectionRegistrationsMaintainer(private val messageRegistrar: RegisterForMessages, private val connectionDependentReceiverRegistrations: Collection<IConnectionDependentReceiverRegistration>) : BroadcastReceiver(), AutoCloseable, ImmediateResponse<List<BroadcastReceiver>, Unit> {
	private var registrationPromise = Promise(emptyList<BroadcastReceiver>())

	@Synchronized
	override fun onReceive(context: Context, intent: Intent) {
		val buildSessionStatus = intent.getIntExtra(SelectedConnection.buildSessionBroadcastStatus, -1)
		if (buildSessionStatus != SelectedConnection.BuildingSessionConnectionStatus.BuildingSessionComplete) return

		registrationPromise = registrationPromise
			.then(this) // remove existing registrations
			.eventually { getInstance(context).promiseSessionConnection() }
			.then { connectionProvider ->
				connectionDependentReceiverRegistrations.map { registration ->
					val receiver = registration.registerWithConnectionProvider(connectionProvider)
					for (i in registration.forIntents())
						messageRegistrar.registerReceiver(receiver, i)

					receiver
				}
			}
	}

	override fun close() {
		registrationPromise.then(this)
	}

	override fun respond(registeredReceivers: List<BroadcastReceiver>) {
		for (receiver in registeredReceivers)
			messageRegistrar.unregisterReceiver(receiver)
	}
}
