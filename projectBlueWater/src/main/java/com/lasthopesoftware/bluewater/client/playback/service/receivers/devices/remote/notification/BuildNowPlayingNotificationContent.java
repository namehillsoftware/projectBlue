package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.notification;

import android.app.Notification;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface BuildNowPlayingNotificationContent {
	Promise<Notification> promiseNowPlayingNotification(ServiceFile serviceFile, boolean isPlaying);
}
