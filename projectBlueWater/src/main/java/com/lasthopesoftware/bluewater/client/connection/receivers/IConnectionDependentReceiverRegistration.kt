package com.lasthopesoftware.bluewater.client.connection.receivers;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;

import java.util.Collection;

/**
 * Created by david on 3/19/17.
 */

public interface IConnectionDependentReceiverRegistration {
	BroadcastReceiver registerWithConnectionProvider(IConnectionProvider connectionProvider);
	Collection<IntentFilter> forIntents();
}
