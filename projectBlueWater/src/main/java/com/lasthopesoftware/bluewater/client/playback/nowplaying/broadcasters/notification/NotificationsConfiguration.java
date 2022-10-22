package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification;

public class NotificationsConfiguration {

	private final String notificationChannel;
	private final int notificationId;

	public NotificationsConfiguration(String notificationChannel, int notificationId) {
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
