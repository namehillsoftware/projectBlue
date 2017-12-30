package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected.IConnectedDeviceBroadcaster;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RemoteControlProxy extends BroadcastReceiver {

	private final Collection<IConnectedDeviceBroadcaster> connectedDeviceBroadcasters;
	private final Map<String, OneParameterAction<Intent>> mappedEvents;

	public RemoteControlProxy(Collection<IConnectedDeviceBroadcaster> connectedDeviceBroadcasters) {
		this.connectedDeviceBroadcasters = connectedDeviceBroadcasters;

		mappedEvents = new HashMap<>(5);
		mappedEvents.put(PlaylistEvents.onPlaylistChange, this::onPlaylistChange);
		mappedEvents.put(PlaylistEvents.onPlaylistPause, i -> Stream.of(connectedDeviceBroadcasters).forEach(IConnectedDeviceBroadcaster::setPaused));
		mappedEvents.put(PlaylistEvents.onPlaylistStart, i -> Stream.of(connectedDeviceBroadcasters).forEach(IConnectedDeviceBroadcaster::setPlaying));
		mappedEvents.put(PlaylistEvents.onPlaylistStop, i -> Stream.of(connectedDeviceBroadcasters).forEach(IConnectedDeviceBroadcaster::setStopped));
		mappedEvents.put(TrackPositionBroadcaster.trackPositionUpdate, this::onTrackPositionUpdate);
	}

	public Set<String> registerForIntents() {
		return mappedEvents.keySet();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		if (action == null) return;

		final OneParameterAction<Intent> eventHandler = mappedEvents.get(action);
		if (eventHandler != null)
			eventHandler.runWith(intent);
	}

	private void onPlaylistChange(Intent intent) {
		final int fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1);
		if (fileKey < 0) return;

		for (final IConnectedDeviceBroadcaster connectedDeviceBroadcaster : connectedDeviceBroadcasters)
			connectedDeviceBroadcaster.updateNowPlaying(new ServiceFile(fileKey));
	}

	private void onTrackPositionUpdate(Intent intent) {
		final long trackPosition = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1);
		if (trackPosition < 0) return;

		for (final IConnectedDeviceBroadcaster connectedDeviceBroadcaster : connectedDeviceBroadcasters)
			connectedDeviceBroadcaster.updateTrackPosition(trackPosition);
	}
}
