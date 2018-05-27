package com.lasthopesoftware.bluewater.client.playback.service.notification;

public class PlaybackNotificationsConfiguration {

	private final String notificationChannel;
	private final int notificationId;

	public PlaybackNotificationsConfiguration(String notificationChannel, int notificationId) {
		this.notificationChannel = notificationChannel;
		this.notificationId = notificationId;
	}

	public int getNotificationId() {
		return notificationId;
	}

	public String getNotificationChannel() {
		return notificationChannel;
	}
}
