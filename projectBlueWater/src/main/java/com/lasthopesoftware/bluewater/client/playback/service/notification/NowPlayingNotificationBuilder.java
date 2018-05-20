package com.lasthopesoftware.bluewater.client.playback.service.notification;

import android.app.Notification;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

public class NowPlayingNotificationBuilder implements BuildNowPlayingNotificationContent {
	@Override
	public Promise<Notification> promiseNowPlayingNotification(ServiceFile serviceFile, boolean isPlaying) {
		return Promise.empty();
	}
}
