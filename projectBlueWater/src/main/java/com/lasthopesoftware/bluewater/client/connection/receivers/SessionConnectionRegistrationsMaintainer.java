package com.lasthopesoftware.bluewater.client.connection.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by david on 3/19/17.
 */

public class SessionConnectionRegistrationsMaintainer extends BroadcastReceiver implements AutoCloseable {

	private final LocalBroadcastManager localBroadcastManager;
	private final Collection<IConnectionDependentReceiverRegistration> connectionDependentReceiverRegistrations;

	private Iterable<BroadcastReceiver> registeredReceivers = Collections.emptySet();

	private Promise<?> registrationPromise = Promise.empty();

	public SessionConnectionRegistrationsMaintainer(LocalBroadcastManager localBroadcastManager, Collection<IConnectionDependentReceiverRegistration> connectionDependentReceiverRegistrations) {
		this.localBroadcastManager = localBroadcastManager;
		this.connectionDependentReceiverRegistrations = connectionDependentReceiverRegistrations;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final int buildSessionStatus = intent.getIntExtra(SessionConnection.buildSessionBroadcastStatus, -1);
		if (buildSessionStatus != SessionConnection.BuildingSessionConnectionStatus.BuildingSessionComplete) return;

		Stream.of(registeredReceivers).forEach(localBroadcastManager::unregisterReceiver);

		registrationPromise = registrationPromise
			.eventually(v -> SessionConnection.getInstance(context).promiseSessionConnection())
			.then(connectionProvider -> {
				registeredReceivers = Stream.of(connectionDependentReceiverRegistrations).map(registration -> {
					final BroadcastReceiver receiver = registration.registerWithConnectionProvider(connectionProvider);
					Stream.of(registration.forIntents()).forEach(i -> localBroadcastManager.registerReceiver(receiver, i));
					return  receiver;
				}).collect(Collectors.toList());
				return null;
			});
	}

	@Override
	public void close() {
		Stream.of(registeredReceivers).forEach(localBroadcastManager::unregisterReceiver);
	}
}
