package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.GivenAStandardNotificationManager.AndTheFileHasChanged;

import static com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.newFakeBuilder;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;

import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter;
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Test;
import org.robolectric.Robolectric;

public class WhenPlaybackStarts extends AndroidContext {

	private static final Notification loadingNotification = new Notification();
	private static final Notification startedNotification = new Notification();
	private static final CreateAndHold<Service> service = new Lazy<>(() -> spy(Robolectric.buildService(PlaybackService.class).get()));
	private static final NotificationManager notificationManager = mock(NotificationManager.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	@Override
	public void before() {
		when(notificationContentBuilder.promiseNowPlayingNotification(new ServiceFile(1), true))
			.thenReturn(new Promise<>(newFakeBuilder(startedNotification)));

		when(notificationContentBuilder.getLoadingNotification(anyBoolean()))
			.thenReturn(newFakeBuilder(loadingNotification));

		final PlaybackNotificationRouter playbackNotificationRouter =
			new PlaybackNotificationRouter(new PlaybackNotificationBroadcaster(
				new NotificationsController(
					service.getObject(),
					notificationManager),
				new NotificationsConfiguration("",43),
				notificationContentBuilder,
				() -> new Promise<>(newFakeBuilder(new Notification()))));

		final Intent playlistChangeIntent = new Intent(PlaylistEvents.onPlaylistTrackChange);
		playlistChangeIntent.putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, 1);
		playbackNotificationRouter.onReceive(playlistChangeIntent);
		playbackNotificationRouter.onReceive(new Intent(PlaylistEvents.onPlaylistStart));
	}

	@Test
	public void thenTheServiceIsStartedInTheForeground() {
		verify(service.getObject()).startForeground(43, loadingNotification);
	}
}
