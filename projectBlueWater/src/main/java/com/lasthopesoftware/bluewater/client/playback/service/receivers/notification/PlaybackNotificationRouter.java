package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotifyOfPlaybackEvents;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class PlaybackNotificationRouter extends BroadcastReceiver {

	private final Map<String, OneParameterAction<Intent>> mappedEvents;

	private final NotifyOfPlaybackEvents playbackNotificationBroadcaster;

	public PlaybackNotificationRouter(NotifyOfPlaybackEvents playbackNotificationBroadcaster) {
		this.playbackNotificationBroadcaster = playbackNotificationBroadcaster;

		mappedEvents = new HashMap<>(4);
		mappedEvents.put(PlaylistEvents.onPlaylistChange, this::onPlaylistChange);
		mappedEvents.put(PlaylistEvents.onPlaylistPause, i -> playbackNotificationBroadcaster.notifyPaused());
		mappedEvents.put(PlaylistEvents.onPlaylistStart, i -> playbackNotificationBroadcaster.notifyPlaying());
		mappedEvents.put(PlaylistEvents.onPlaylistStop, i -> playbackNotificationBroadcaster.notifyStopped());
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
		if (fileKey >= 0)
			playbackNotificationBroadcaster.notifyPlayingFileChanged(new ServiceFile(fileKey));
	}
}
