package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.notification.specs.GivenAStandardNotificationManager;

import android.app.NotificationManager;
import android.app.Service;

import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.notification.NotificationBroadcaster;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WhenSettingPlaying {

	private static final Service service = mock(Service.class);
	private static final NotificationManager notificationManager = mock(NotificationManager.class);

	@BeforeClass
	public static void context() {
		final NotificationBroadcaster notificationBroadcaster = new NotificationBroadcaster(service, notificationManager);
		notificationBroadcaster.setPlaying();
	}

	@Test
	public void thenThePlayingNotificationIsStartedForeground() {
		verify(service).startForeground(anyInt(), any());
	}
}
