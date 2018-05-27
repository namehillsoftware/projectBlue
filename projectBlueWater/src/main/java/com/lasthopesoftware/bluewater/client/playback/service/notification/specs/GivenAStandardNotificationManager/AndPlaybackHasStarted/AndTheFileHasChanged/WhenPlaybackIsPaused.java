package com.lasthopesoftware.bluewater.client.playback.service.notification.specs.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.bluewater.shared.specs.AndroidContext;
import com.lasthopesoftware.resources.notifications.specs.FakeNotificationCompatBuilder;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Test;
import org.robolectric.Robolectric;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenPlaybackIsPaused extends AndroidContext {
	private static final Notification pausedNotification = new Notification();
	private static final CreateAndHold<Service> service = new Lazy<>(() -> spy(Robolectric.buildService(PlaybackService.class).get()));
	private static final NotificationManager notificationManager = mock(NotificationManager.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	@Override
	public void before() {
		when(notificationContentBuilder.promiseNowPlayingNotification(new ServiceFile(1), true))
			.thenReturn(new Promise<>(new FakeNotificationCompatBuilder(new Notification())));

		when(notificationContentBuilder.promiseNowPlayingNotification(new ServiceFile(1), false))
			.thenReturn(new Promise<>(new FakeNotificationCompatBuilder(pausedNotification)));

		final PlaybackNotificationBroadcaster playbackNotificationBroadcaster =
			new PlaybackNotificationBroadcaster(
				service.getObject(),
				notificationManager,
				new PlaybackNotificationsConfiguration("",43),
				notificationContentBuilder);

		playbackNotificationBroadcaster.notifyPlaying();
		playbackNotificationBroadcaster.notifyPlayingFileChanged(new ServiceFile(1));
		playbackNotificationBroadcaster.notifyPaused();
	}

	@Test
	public void thenTheServiceContinuesInTheBackground() {
		verify(service.getObject()).stopForeground(false);
	}

	@Test
	public void thenTheNotificationIsSetToThePausedNotification() {
		verify(notificationManager).notify(43, pausedNotification);
	}
}
