package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected.IConnectedDeviceBroadcaster;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RemoteControlProxy extends BroadcastReceiver {

	private final IConnectedDeviceBroadcaster connectedDeviceBroadcaster;

	public RemoteControlProxy(IConnectedDeviceBroadcaster connectedDeviceBroadcaster) {
		this.connectedDeviceBroadcaster = connectedDeviceBroadcaster;
	}

	public Set<String> registerForIntents() {
		return new HashSet<>(
			Arrays.asList(
				PlaylistEvents.onPlaylistChange,
				PlaylistEvents.onPlaylistPause,
				PlaylistEvents.onPlaylistStart,
				PlaylistEvents.onPlaylistStop,
				TrackPositionBroadcaster.trackPositionUpdate));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();

		if (PlaylistEvents.onPlaylistChange.equals(action)) {
			final int fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1);
			if (fileKey > 0)
				connectedDeviceBroadcaster.updateNowPlaying(new ServiceFile(fileKey));

			return;
		}

		if (PlaylistEvents.onPlaylistPause.equals(action)) {
			connectedDeviceBroadcaster.setPaused();
			return;
		}

		if (PlaylistEvents.onPlaylistStart.equals(action)) {
			connectedDeviceBroadcaster.setPlaying();
			return;
		}

		if (PlaylistEvents.onPlaylistStop.equals(action)) {
			connectedDeviceBroadcaster.setStopped();
			return;
		}

		if (TrackPositionBroadcaster.trackPositionUpdate.equals(action)) {
			final int trackPosition = intent.getIntExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1);
			if (trackPosition >= 0)
				connectedDeviceBroadcaster.updateTrackPosition(trackPosition);
		}
	}
}
