package com.lasthopesoftware.bluewater.client.connection.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

/**
 * Created by david on 3/19/17.
 */
class SessionConnectionRegistrationsMaintainer(private val localBroadcastManager: LocalBroadcastManager, private val connectionDependentReceiverRegistrations: Collection<IConnectionDependentReceiverRegistration>) : BroadcastReceiver(), AutoCloseable, ImmediateResponse<List<BroadcastReceiver>, Unit> {
	private var registrationPromise = Promise(emptyList<BroadcastReceiver>())

	@Synchronized
	override fun onReceive(context: Context, intent: Intent) {
		val buildSessionStatus = intent.getIntExtra(SelectedConnection.buildSessionBroadcastStatus, -1)
		if (buildSessionStatus != SelectedConnection.BuildingSessionConnectionStatus.BuildingSessionComplete) return

		registrationPromise = registrationPromise
			.then(this)
			.eventually { getInstance(context).promiseSessionConnection() }
			.then { connectionProvider ->
				connectionDependentReceiverRegistrations.map { registration ->
					val receiver = registration.registerWithConnectionProvider(connectionProvider)
					for (i in registration.forIntents())
						localBroadcastManager.registerReceiver(receiver, i)

					receiver
				}
			}
	}

	override fun close() {
		registrationPromise.then(this)
	}

	override fun respond(registeredReceivers: List<BroadcastReceiver>) {
		for (receiver in registeredReceivers)
			localBroadcastManager.unregisterReceiver(receiver)
	}
}
