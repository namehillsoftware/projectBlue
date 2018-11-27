package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RemoteControlProxy extends BroadcastReceiver {

	private final IRemoteBroadcaster remoteBroadcaster;
	private final Map<String, OneParameterAction<Intent>> mappedEvents;

	public RemoteControlProxy(IRemoteBroadcaster remoteBroadcaster) {
		this.remoteBroadcaster = remoteBroadcaster;

		mappedEvents = new HashMap<>(5);
		mappedEvents.put(PlaylistEvents.onPlaylistChange, this::onPlaylistChange);
		mappedEvents.put(PlaylistEvents.onPlaylistPause, i -> remoteBroadcaster.setPaused());
		mappedEvents.put(PlaylistEvents.onPlaylistStart, i -> remoteBroadcaster.setPlaying());
		mappedEvents.put(PlaylistEvents.onPlaylistStop, i -> remoteBroadcaster.setStopped());
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
		if (fileKey > 0)
			remoteBroadcaster.updateNowPlaying(new ServiceFile(fileKey));
	}

	private void onTrackPositionUpdate(Intent intent) {
		final long trackPosition = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1);
		if (trackPosition >= 0)
			remoteBroadcaster.updateTrackPosition(trackPosition);
	}
}
