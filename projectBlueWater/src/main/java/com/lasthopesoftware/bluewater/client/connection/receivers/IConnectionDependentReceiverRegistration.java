package com.lasthopesoftware.bluewater.client.connection.receivers;

import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;

/**
 * Created by david on 3/19/17.
 */

public interface IConnectionDependentReceiverRegistration {
	void registerWithConnectionProvider(LocalBroadcastManager localBroadcastManager, IConnectionProvider connectionProvider);
}
