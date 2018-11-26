package com.lasthopesoftware.bluewater.client.playback.service.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.namehillsoftware.handoff.promises.response.VoidResponse;


public class PlaybackNotificationBroadcaster implements NotifyOfPlaybackEvents {

	private final Service service;
	private final NotificationManager notificationManager;
	private final PlaybackNotificationsConfiguration playbackNotificationsConfiguration;
	private final BuildNowPlayingNotificationContent nowPlayingNotificationContentBuilder;

	private final Object notificationSync = new Object();
	private boolean isPlaying;
	private boolean isNotificationStarted;
	private boolean isNotificationForeground;
	private ServiceFile serviceFile;

	public PlaybackNotificationBroadcaster(Service service, NotificationManager notificationManager, PlaybackNotificationsConfiguration playbackNotificationsConfiguration, BuildNowPlayingNotificationContent nowPlayingNotificationContentBuilder) {
		this.service = service;
		this.notificationManager = notificationManager;
		this.playbackNotificationsConfiguration = playbackNotificationsConfiguration;
		this.nowPlayingNotificationContentBuilder = nowPlayingNotificationContentBuilder;
	}

	@Override
	public void notifyPlaying() {
		synchronized (notificationSync) {
			isPlaying = true;

			if (serviceFile != null)
				updateNowPlaying(serviceFile);
		}
	}

	@Override
	public void notifyPaused() {
		synchronized (notificationSync) {
			if (serviceFile == null) {
				service.stopForeground(false);
				isNotificationForeground = false;
				return;
			}

			nowPlayingNotificationContentBuilder.promiseNowPlayingNotification(serviceFile, isPlaying = false)
				.then(builder -> {
					final Notification notification = builder.build();
					synchronized (notificationSync) {
						notificationManager.notify(playbackNotificationsConfiguration.getNotificationId(), notification);
						service.stopForeground(false);
						isNotificationForeground = false;
						return null;
					}
				});
		}
	}

	@Override
	public void notifyStopped() {
		synchronized (notificationSync) {
			isPlaying = false;
			service.stopForeground(true);
			isNotificationStarted = false;
			isNotificationForeground = false;
		}
	}

	@Override
	public void notifyPlayingFileChanged(ServiceFile serviceFile) {
		updateNowPlaying(serviceFile);
	}

	private void updateNowPlaying(ServiceFile serviceFile) {
		synchronized (notificationSync) {
			this.serviceFile = serviceFile;

			if (!isNotificationStarted && !isPlaying) return;

			nowPlayingNotificationContentBuilder.promiseNowPlayingNotification(serviceFile, isPlaying)
				.then(new VoidResponse<>(builder -> {
					final Notification notification = builder.build();
					synchronized (notificationSync) {
						if (!isPlaying || isNotificationForeground) {
							if (!isNotificationStarted) return;

							notificationManager.notify(
								playbackNotificationsConfiguration.getNotificationId(),
								notification);
							return;
						}

						service.startForeground(playbackNotificationsConfiguration.getNotificationId(), notification);
						isNotificationStarted = true;
						isNotificationForeground = true;
					}
				}));
		}
	}
}
