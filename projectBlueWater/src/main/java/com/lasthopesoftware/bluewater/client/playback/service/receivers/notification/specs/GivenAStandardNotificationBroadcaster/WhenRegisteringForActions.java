package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.specs.GivenAStandardNotificationBroadcaster;

import android.app.NotificationManager;

import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationBroadcaster;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class WhenRegisteringForActions {

	private static Collection<String> registeredIntents;

	@Before
	public void context() {
		final PlaybackNotificationBroadcaster playbackNotificationBroadcaster =
			new PlaybackNotificationBroadcaster(
				Robolectric.buildService(PlaybackService.class).get(),
				mock(NotificationManager.class),
				new PlaybackNotificationsConfiguration(3),
				mock(BuildNowPlayingNotificationContent.class));

		registeredIntents = playbackNotificationBroadcaster.registerForIntents();
	}

	@Test
	public void thenTheRegisteredActionsAreCorrect() {
		assertThat(registeredIntents).isSubsetOf(
			PlaylistEvents.onPlaylistChange,
			PlaylistEvents.onPlaylistPause,
			PlaylistEvents.onPlaylistStart,
			PlaylistEvents.onPlaylistStop);
	}
}
