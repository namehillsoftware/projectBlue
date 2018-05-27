package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.specs.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsPaused.ThenStartedAgain;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class WhenTheFileChanges {

	private static final Notification firstNotification = new Notification();
	private static final Notification secondNotification = new Notification();
	private static final CreateAndHold<Service> service = new Lazy<>(() -> spy(Robolectric.buildService(PlaybackService.class).get()));
	private static final NotificationManager notificationManager = mock(NotificationManager.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	private static final CreateAndHold<Object> testSetup = new AbstractSynchronousLazy<Object>() {
		@Override
		protected Object create() {

			when(notificationContentBuilder.promiseNowPlayingNotification(
					argThat(arg -> new ServiceFile(1).equals(arg)),
					anyBoolean()))
				.thenReturn(new Promise<>(firstNotification));

			when(notificationContentBuilder.promiseNowPlayingNotification(
					argThat(arg -> new ServiceFile(2).equals(arg)),
					anyBoolean()))
				.thenReturn(new Promise<>(secondNotification));

			final PlaybackNotificationRouter playbackNotificationRouter =
				new PlaybackNotificationRouter(new PlaybackNotificationBroadcaster(
					service.getObject(),
					notificationManager,
					new PlaybackNotificationsConfiguration("",43),
					notificationContentBuilder));

			playbackNotificationRouter
				.onReceive(
					RuntimeEnvironment.application,
					new Intent(PlaylistEvents.onPlaylistStart));

			{
				final Intent playlistChangeIntent = new Intent(PlaylistEvents.onPlaylistChange);
				playlistChangeIntent.putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, 1);
				playbackNotificationRouter
					.onReceive(
						RuntimeEnvironment.application,
						playlistChangeIntent);
			}

			playbackNotificationRouter
				.onReceive(
					RuntimeEnvironment.application,
					new Intent(PlaylistEvents.onPlaylistPause));

			{
				final Intent playlistChangeIntent = new Intent(PlaylistEvents.onPlaylistChange);
				playlistChangeIntent.putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, 2);
				playbackNotificationRouter
					.onReceive(
						RuntimeEnvironment.application,
						playlistChangeIntent);
			}

			playbackNotificationRouter
				.onReceive(
					RuntimeEnvironment.application,
					new Intent(PlaylistEvents.onPlaylistStart));

			return new Object();
		}
	};

	@Before
	public void context() {
		testSetup.getObject();
	}

	@Test
	public void thenTheServiceIsStartedOnTheFirstServiceItem() {
		verify(service.getObject(), times(1))
			.startForeground(43, firstNotification);
	}

	@Test
	public void thenTheServiceContinuesInTheBackground() {
		verify(service.getObject()).stopForeground(false);
	}

	@Test
	public void thenTheNotificationIsSetToThePausedNotification() {
		verify(notificationManager).notify(43, secondNotification);
	}

	@Test
	public void thenTheServiceIsStartedOnTheSecondServiceItem() {
		verify(service.getObject(), times(1))
			.startForeground(43, secondNotification);
	}
}
