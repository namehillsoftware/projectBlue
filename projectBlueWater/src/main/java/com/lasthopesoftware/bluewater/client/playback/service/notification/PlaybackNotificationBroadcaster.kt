package com.lasthopesoftware.bluewater.client.playback.service.notification;

import android.app.Notification;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildPlaybackStartingNotification;
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications;
import com.namehillsoftware.handoff.promises.response.VoidResponse;


public class PlaybackNotificationBroadcaster implements NotifyOfPlaybackEvents {

	private final ControlNotifications notificationsController;
	private final BuildNowPlayingNotificationContent nowPlayingNotificationContentBuilder;
	private final int notificationId;
	private final BuildPlaybackStartingNotification playbackStartingNotification;

	private final Object notificationSync = new Object();
	private boolean isPlaying;
	private boolean isNotificationStarted;
	private ServiceFile serviceFile;

	public PlaybackNotificationBroadcaster(ControlNotifications notificationsController, NotificationsConfiguration notificationsConfiguration, BuildNowPlayingNotificationContent nowPlayingNotificationContentBuilder, BuildPlaybackStartingNotification playbackStartingNotification) {
		this.notificationsController = notificationsController;
		this.notificationId = notificationsConfiguration.getNotificationId();
		this.nowPlayingNotificationContentBuilder = nowPlayingNotificationContentBuilder;
		this.playbackStartingNotification = playbackStartingNotification;
	}

	@Override
	public void notifyPlaying() {
		isPlaying = true;

		if (serviceFile != null) {
			updateNowPlaying(serviceFile);
			return;
		}

		playbackStartingNotification.promisePreparedPlaybackStartingNotification()
			.then(new VoidResponse<>(builder -> {
				synchronized (notificationSync) {
					if (isNotificationStarted) return;

					isNotificationStarted = true;
					notificationsController.notifyForeground(builder.build(), notificationId);
				}
			}));
	}

	@Override
	public void notifyPaused() {
		if (serviceFile == null) {
			notificationsController.stopForegroundNotification(notificationId);
			return;
		}

		nowPlayingNotificationContentBuilder.promiseNowPlayingNotification(serviceFile, isPlaying = false)
			.then(builder -> {
				notificationsController.notifyBackground(builder.build(), notificationId);
				return null;
			});
	}

	@Override
	public void notifyStopped() {
		synchronized (notificationSync) {
			isPlaying = false;
			isNotificationStarted = false;
			notificationsController.removeNotification(notificationId);
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

			final Notification loadingBuilderNotification = nowPlayingNotificationContentBuilder.getLoadingNotification(isPlaying).build();
			if (isPlaying) {
				notificationsController.notifyForeground(loadingBuilderNotification, notificationId);
				isNotificationStarted = true;
			}

			if (!isPlaying && isNotificationStarted) {
				notificationsController.notifyBackground(loadingBuilderNotification, notificationId);
			}

			nowPlayingNotificationContentBuilder.promiseNowPlayingNotification(serviceFile, isPlaying)
				.then(new VoidResponse<>(builder -> {
					synchronized (notificationSync) {
						if (!isPlaying) {
							if (!isNotificationStarted) return;

							notificationsController.notifyBackground(builder.build(), notificationId);
							return;
						}

						isNotificationStarted = true;
						notificationsController.notifyForeground(builder.build(), notificationId);
					}
				}));
		}
	}
}
