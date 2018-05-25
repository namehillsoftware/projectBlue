package com.lasthopesoftware.bluewater.client.playback.service.notification;

import android.support.v4.media.session.MediaSessionCompat;

public class PlaybackNotificationsConfiguration {

	private final String notificationChannel;
	private final int notificationId;
	private final MediaSessionCompat.Token mediaSessionToken;

	public PlaybackNotificationsConfiguration(String notificationChannel, int notificationId, MediaSessionCompat.Token mediaSessionToken) {
		this.notificationChannel = notificationChannel;
		this.notificationId = notificationId;
		this.mediaSessionToken = mediaSessionToken;
	}

	public int getNotificationId() {
		return notificationId;
	}

	public String getNotificationChannel() {
		return notificationChannel;
	}

	public  MediaSessionCompat.Token getMediaSessionToken() {
		return mediaSessionToken;
	}
}
