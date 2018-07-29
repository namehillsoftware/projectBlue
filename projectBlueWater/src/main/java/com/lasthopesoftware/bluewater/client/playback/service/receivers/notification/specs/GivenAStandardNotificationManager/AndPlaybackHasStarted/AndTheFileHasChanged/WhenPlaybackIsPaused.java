package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.specs.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

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
		final NotificationCompat.Builder builder = mock(NotificationCompat.Builder.class);
		when(builder.build())
			.thenReturn(new Notification())
			.thenReturn(pausedNotification);

		when(notificationContentBuilder.promiseNowPlayingNotification(new ServiceFile(1), true))
			.thenReturn(new Promise<>(builder));

		when(notificationContentBuilder.promiseNowPlayingNotification(new ServiceFile(1), false))
			.thenReturn(new Promise<>(builder));

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
