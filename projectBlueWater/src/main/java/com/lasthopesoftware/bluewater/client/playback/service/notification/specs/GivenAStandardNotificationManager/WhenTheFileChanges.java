package com.lasthopesoftware.bluewater.client.playback.service.notification.specs.GivenAStandardNotificationManager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.BuildNowPlayingNotificationContent;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class WhenTheFileChanges {

	private static final CreateAndHold<Service> service = new Lazy<>(() -> spy(Robolectric.buildService(PlaybackService.class).get()));
	private static final NotificationManager notificationManager = mock(NotificationManager.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	private static final CreateAndHold<Void> testSetup = new AbstractSynchronousLazy<Void>() {
		@Override
		protected Void create() throws Throwable {

			when(notificationContentBuilder.promiseNowPlayingNotification(any(), anyBoolean()))
				.thenReturn(new Promise<>(new Notification()));

			final PlaybackNotificationBroadcaster playbackNotificationBroadcaster =
				new PlaybackNotificationBroadcaster(
					service.getObject(),
					notificationManager,
					new PlaybackNotificationsConfiguration(43),
					notificationContentBuilder);

			playbackNotificationBroadcaster.notifyPlayingFileChanged(new ServiceFile(1));

			return null;
		}
	};

	@Before
	public void context() {
		testSetup.getObject();
	}

	@Test
	public void thenTheServiceHasNotStarted() {
		verify(service.getObject(), never()).startForeground(anyInt(), any());
	}

	@Test
	public void thenTheNotificationHasNotBeenBroadcast() {
		verify(notificationManager, never()).notify(anyInt(), any());
	}
}
