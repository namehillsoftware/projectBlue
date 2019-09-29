package com.lasthopesoftware.bluewater.client.playback.service.notification;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.resources.notifications.control.ControlNotifications;
import com.namehillsoftware.handoff.promises.response.VoidResponse;


public class PlaybackNotificationBroadcaster implements NotifyOfPlaybackEvents {

	private final ControlNotifications notificationsController;
	private final PlaybackNotificationsConfiguration playbackNotificationsConfiguration;
	private final BuildNowPlayingNotificationContent nowPlayingNotificationContentBuilder;

	private final Object notificationSync = new Object();
	private boolean isPlaying;
	private boolean isNotificationStarted;
	private ServiceFile serviceFile;

	public PlaybackNotificationBroadcaster(ControlNotifications notificationsController, PlaybackNotificationsConfiguration playbackNotificationsConfiguration, BuildNowPlayingNotificationContent nowPlayingNotificationContentBuilder) {
		this.notificationsController = notificationsController;
		this.playbackNotificationsConfiguration = playbackNotificationsConfiguration;
		this.nowPlayingNotificationContentBuilder = nowPlayingNotificationContentBuilder;
	}

	@Override
	public void notifyPlaying() {
		isPlaying = true;

		if (serviceFile != null)
			updateNowPlaying(serviceFile);
	}

	@Override
	public void notifyPaused() {
		if (serviceFile == null) {
			notificationsController.stopForegroundNotification(playbackNotificationsConfiguration.getNotificationId());
			return;
		}

		nowPlayingNotificationContentBuilder.promiseNowPlayingNotification(serviceFile, isPlaying = false)
			.then(builder -> {
				notificationsController.notifyBackground(builder.build(), playbackNotificationsConfiguration.getNotificationId());
				return null;
			});
	}

	@Override
	public void notifyStopped() {
		synchronized (notificationSync) {
			isPlaying = false;
			isNotificationStarted = false;
			notificationsController.removeForegroundNotification(playbackNotificationsConfiguration.getNotificationId());
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
					synchronized (notificationSync) {
						if (!isPlaying) {
							if (!isNotificationStarted) return;

							notificationsController.notifyBackground(builder.build(), playbackNotificationsConfiguration.getNotificationId());
							return;
						}

						isNotificationStarted = true;
						notificationsController.notifyForeground(builder.build(), playbackNotificationsConfiguration.getNotificationId());
					}
				}));
		}
	}
}
