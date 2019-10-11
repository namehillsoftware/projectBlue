package com.lasthopesoftware.bluewater.client.playback.service.notification.building;

import androidx.core.app.NotificationCompat;

import com.namehillsoftware.handoff.promises.Promise;

public interface BuildPlaybackStartingNotification {
	Promise<NotificationCompat.Builder> promisePreparedPlaybackStartingNotification();
}
