package com.lasthopesoftware.bluewater.client.playback.service.notification.building;

import android.support.v4.app.NotificationCompat;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface BuildNowPlayingNotificationContent {
	Promise<NotificationCompat.Builder> promiseNowPlayingNotification(ServiceFile serviceFile, boolean isPlaying);
}
