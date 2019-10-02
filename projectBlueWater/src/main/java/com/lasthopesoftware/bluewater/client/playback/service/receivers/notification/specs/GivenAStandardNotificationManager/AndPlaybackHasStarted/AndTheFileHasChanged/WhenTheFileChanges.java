package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.specs.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter;
import com.lasthopesoftware.resources.notifications.control.NotificationsController;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenTheFileChanges extends AndroidContext {

	private static final Notification startedNotification = new Notification();
	private static final Notification nextNotification = new Notification();
	private static final CreateAndHold<Service> service = new Lazy<>(() -> spy(Robolectric.buildService(PlaybackService.class).get()));
	private static final NotificationManager notificationManager = mock(NotificationManager.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	@Override
	public void before() {
		final NotificationCompat.Builder startedBuilder = mock(NotificationCompat.Builder.class);
		when(startedBuilder.build()).thenReturn(startedNotification);
		when(notificationContentBuilder.promiseNowPlayingNotification(any(), anyBoolean()))
			.thenReturn(new Promise<>(startedBuilder));

		final NotificationCompat.Builder nextBuilder = mock(NotificationCompat.Builder.class);
		when(nextBuilder.build()).thenReturn(nextNotification);
		when(notificationContentBuilder.promiseNowPlayingNotification(new ServiceFile(2), true))
			.thenReturn(new Promise<>(nextBuilder));

		final PlaybackNotificationRouter playbackNotificationRouter =
			new PlaybackNotificationRouter(new PlaybackNotificationBroadcaster(
				new NotificationsController(
					service.getObject(),
					notificationManager),
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
	}

	@Test
	public void thenTheServiceIsStartedInTheForeground() {
		verify(service.getObject()).startForeground(43, startedNotification);
	}
}
