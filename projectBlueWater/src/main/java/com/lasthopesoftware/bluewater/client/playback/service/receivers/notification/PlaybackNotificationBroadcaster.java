package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.namehillsoftware.handoff.promises.response.ImmediateAction.perform;


public class PlaybackNotificationBroadcaster extends BroadcastReceiver {

	private final Map<String, OneParameterAction<Intent>> mappedEvents;

	private final Service service;
	private final NotificationManager notificationManager;
	private final PlaybackNotificationsConfiguration playbackNotificationsConfiguration;
	private final BuildNowPlayingNotificationContent nowPlayingNotificationContentBuilder;

	private volatile boolean isPlaying;
	private volatile boolean isNotificationStarted;
	private volatile boolean isNotificationForeground;
	private volatile ServiceFile serviceFile;

	public PlaybackNotificationBroadcaster(Service service, NotificationManager notificationManager, PlaybackNotificationsConfiguration playbackNotificationsConfiguration, BuildNowPlayingNotificationContent nowPlayingNotificationContentBuilder) {
		this.service = service;
		this.notificationManager = notificationManager;
		this.playbackNotificationsConfiguration = playbackNotificationsConfiguration;
		this.nowPlayingNotificationContentBuilder = nowPlayingNotificationContentBuilder;

		mappedEvents = new HashMap<>(4);
		mappedEvents.put(PlaylistEvents.onPlaylistChange, this::onPlaylistChange);
		mappedEvents.put(PlaylistEvents.onPlaylistPause, i -> setPaused());
		mappedEvents.put(PlaylistEvents.onPlaylistStart, i -> setPlaying());
		mappedEvents.put(PlaylistEvents.onPlaylistStop, i -> setStopped());
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

	private void setPlaying() {
		isPlaying = true;

		if (serviceFile != null)
			updateNowPlaying(serviceFile);
	}

	private void setPaused() {
		if (serviceFile == null) {
			service.stopForeground(false);
			isNotificationForeground = false;
			return;
		}

		nowPlayingNotificationContentBuilder.promiseNowPlayingNotification(serviceFile, isPlaying = false)
			.then(notification -> {
				notificationManager.notify(playbackNotificationsConfiguration.getNotificationId(), notification);
				service.stopForeground(false);
				isNotificationForeground = false;
				return null;
			});
	}

	private void setStopped() {
		isPlaying = false;
		service.stopForeground(true);
		isNotificationStarted = false;
		isNotificationForeground = false;
	}

	private void onPlaylistChange(Intent intent) {
		final int fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1);
		if (fileKey >= 0)
			updateNowPlaying(new ServiceFile(fileKey));
	}

	private void updateNowPlaying(ServiceFile serviceFile) {
		this.serviceFile = serviceFile;

		if (!isNotificationStarted && !isPlaying) return;

		nowPlayingNotificationContentBuilder.promiseNowPlayingNotification(serviceFile, isPlaying)
			.then(perform(notification -> {
				if (!isPlaying || (isNotificationStarted && isNotificationForeground)) {
					notificationManager.notify(
						playbackNotificationsConfiguration.getNotificationId(),
						notification);
					return;
				}

				service.startForeground(playbackNotificationsConfiguration.getNotificationId(), notification);
				isNotificationStarted = true;
				isNotificationForeground = true;
			}));
	}
}
