package com.lasthopesoftware.bluewater.client.playback.service.notification.specs.GivenAStandardNotificationManager;

import android.app.Notification;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildPlaybackStartingNotification;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter;
import com.lasthopesoftware.resources.notifications.control.ControlNotifications;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;

import static com.lasthopesoftware.resources.notifications.specs.FakeNotificationCompatBuilder.newFakeBuilder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenPlaybackStarts {
	private static final Notification startedNotification = new Notification();
	private static final ControlNotifications notificationController = mock(ControlNotifications.class);

	public void before() {
		final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

		final BuildPlaybackStartingNotification startingNotification = mock(BuildPlaybackStartingNotification.class);
		when(startingNotification.promisePreparedPlaybackStartingNotification())
			.thenReturn(new Promise<>(newFakeBuilder(startedNotification)));

		final PlaybackNotificationRouter playbackNotificationRouter =
			new PlaybackNotificationRouter(new PlaybackNotificationBroadcaster(
				notificationController,
				new PlaybackNotificationsConfiguration("",43),
				notificationContentBuilder,
				startingNotification));

		playbackNotificationRouter
			.onReceive(
				ApplicationProvider.getApplicationContext(),
				new Intent(PlaylistEvents.onPlaylistStart));
	}

	@Test
	public void thenAStartingNotificationIsSet() {
		verify(notificationController, times(1)).notifyForeground(startedNotification, 43);
	}
}
