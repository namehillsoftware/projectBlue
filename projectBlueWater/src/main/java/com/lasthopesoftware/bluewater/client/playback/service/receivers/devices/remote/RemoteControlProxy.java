package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected.IConnectedDeviceBroadcaster;

public class RemoteControlProxy extends BroadcastReceiver {

	private final IConnectedDeviceBroadcaster connectedDeviceBroadcaster;

	public RemoteControlProxy(IConnectedDeviceBroadcaster connectedDeviceBroadcaster) {
		this.connectedDeviceBroadcaster = connectedDeviceBroadcaster;
	}

	@Override
	public void onReceive(Context context, Intent intent) {

	}
}
