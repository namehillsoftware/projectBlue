package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.notification;

import android.app.NotificationManager;
import android.app.Service;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.IRemoteBroadcaster;


public class NotificationBroadcaster implements IRemoteBroadcaster {

	private final Service service;
	private final NotificationManager notificationManager;
	private final PlaybackNotificationsConfiguration playbackNotificationsConfiguration;
	private final BuildNowPlayingNotificationContent nowPlayingNotificationContentBuilder;

	private volatile boolean isPlaying;

	public NotificationBroadcaster(Service service, NotificationManager notificationManager, PlaybackNotificationsConfiguration playbackNotificationsConfiguration, BuildNowPlayingNotificationContent nowPlayingNotificationContentBuilder) {
		this.service = service;
		this.notificationManager = notificationManager;
		this.playbackNotificationsConfiguration = playbackNotificationsConfiguration;
		this.nowPlayingNotificationContentBuilder = nowPlayingNotificationContentBuilder;
	}

	@Override
	public void setPlaying() {
		isPlaying = true;
	}

	@Override
	public void setPaused() {

	}

	@Override
	public void setStopped() {

	}

	@Override
	public void updateNowPlaying(ServiceFile serviceFile) {
		nowPlayingNotificationContentBuilder.promiseNowPlayingNotification(serviceFile, isPlaying)
			.then(notification -> {
				service.startForeground(playbackNotificationsConfiguration.getNotificationId(), notification);
				return null;
			});
	}

	@Override
	public void updateTrackPosition(long trackPosition) {

	}
}
