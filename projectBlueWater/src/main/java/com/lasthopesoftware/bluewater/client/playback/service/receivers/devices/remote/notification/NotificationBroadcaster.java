package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.IRemoteBroadcaster;


public class NotificationBroadcaster implements IRemoteBroadcaster {

	private static final int notificationId = 42;
	private final Service service;
	private final NotificationManager notificationManager;

	public NotificationBroadcaster(Service service, NotificationManager notificationManager) {
		this.service = service;
		this.notificationManager = notificationManager;
	}

	@Override
	public void setPlaying() {
		service.startForeground(notificationId, new Notification());
	}

	@Override
	public void setPaused() {

	}

	@Override
	public void setStopped() {

	}

	@Override
	public void updateNowPlaying(ServiceFile serviceFile) {

	}

	@Override
	public void updateTrackPosition(long trackPosition) {

	}
}
