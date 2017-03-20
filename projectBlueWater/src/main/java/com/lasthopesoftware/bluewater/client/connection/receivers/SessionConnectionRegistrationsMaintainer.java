package com.lasthopesoftware.bluewater.client.connection.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;

import java.util.Collection;

/**
 * Created by david on 3/19/17.
 */

public class SessionConnectionRegistrationsMaintainer extends BroadcastReceiver {

	private final LocalBroadcastManager localBroadcastManager;
	private final Collection<IConnectionDependentReceiverRegistration> connectionDependentReceiverRegistrations;

	public SessionConnectionRegistrationsMaintainer(LocalBroadcastManager localBroadcastManager, Collection<IConnectionDependentReceiverRegistration> connectionDependentReceiverRegistrations) {
		this.localBroadcastManager = localBroadcastManager;
		this.connectionDependentReceiverRegistrations = connectionDependentReceiverRegistrations;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final int buildSessionStatus = intent.getIntExtra(SessionConnection.buildSessionBroadcastStatus, -1);
		if (buildSessionStatus != SessionConnection.BuildingSessionConnectionStatus.BuildingSessionComplete) return;

		final IConnectionProvider connectionProvider = SessionConnection.getSessionConnectionProvider();
		Stream.of(connectionDependentReceiverRegistrations).forEach(registration -> registration.registerWithConnectionProvider(localBroadcastManager, connectionProvider));
	}
}
