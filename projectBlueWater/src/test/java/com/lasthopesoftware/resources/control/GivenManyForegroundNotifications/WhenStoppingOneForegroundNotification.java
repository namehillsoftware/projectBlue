package com.lasthopesoftware.resources.control.GivenManyForegroundNotifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;

import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.resources.notifications.control.NotificationsController;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Test;
import org.robolectric.Robolectric;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class WhenStoppingOneForegroundNotification extends AndroidContext {

	private static final CreateAndHold<Service> service = new Lazy<>(() -> spy(Robolectric.buildService(PlaybackService.class).get()));
	private static final NotificationManager notificationManager = mock(NotificationManager.class);

	@Override
	public void before() throws Exception {
		final NotificationsController notificationsController = new NotificationsController(service.getObject(), notificationManager);

		notificationsController.notifyForeground(mock(Notification.class), 13);
		notificationsController.notifyForeground(mock(Notification.class), 33);
		notificationsController.notifyForeground(mock(Notification.class), 77);

		notificationsController.stopForegroundNotification(13);
	}

	@Test
	public void thenTheServiceStartsForegroundForEachForegroundNotification() {
		verify(service.getObject(), times(3)).startForeground(anyInt(), any());
	}

	@Test
	public void thenTheServiceIsStillInTheForeground() {
		verify(service.getObject(), never()).stopForeground(anyBoolean());
	}
}
