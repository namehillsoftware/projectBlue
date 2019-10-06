package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.specs.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsPaused.ThenStartedAgain;

import android.app.Notification;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter;
import com.lasthopesoftware.resources.notifications.control.ControlNotifications;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;

import static com.lasthopesoftware.resources.notifications.specs.FakeNotificationCompatBuilder.newFakeBuilder;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenTheFileChanges extends AndroidContext {

	private static final Notification loadingNotification = new Notification();
	private static final Notification firstNotification = new Notification();
	private static final Notification secondNotification = new Notification();
	private static final ControlNotifications notificationsController = mock(ControlNotifications.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	@Override
	public void before() {
		when(notificationContentBuilder.getLoadingNotification(anyBoolean()))
			.thenReturn(newFakeBuilder(loadingNotification));

		when(notificationContentBuilder.promiseNowPlayingNotification(
			argThat(arg -> new ServiceFile(1).equals(arg)),
			anyBoolean()))
			.thenReturn(new Promise<>(newFakeBuilder(firstNotification)));

		when(notificationContentBuilder.promiseNowPlayingNotification(
			argThat(arg -> new ServiceFile(2).equals(arg)),
			anyBoolean()))
			.thenReturn(new Promise<>(newFakeBuilder(secondNotification)));

		final PlaybackNotificationRouter playbackNotificationRouter =
			new PlaybackNotificationRouter(new PlaybackNotificationBroadcaster(
				notificationsController,
				new PlaybackNotificationsConfiguration("",43),
				notificationContentBuilder));

		playbackNotificationRouter
			.onReceive(
				ApplicationProvider.getApplicationContext(),
				new Intent(PlaylistEvents.onPlaylistStart));

		{
			final Intent playlistChangeIntent = new Intent(PlaylistEvents.onPlaylistChange);
			playlistChangeIntent.putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, 1);
			playbackNotificationRouter
				.onReceive(
					ApplicationProvider.getApplicationContext(),
					playlistChangeIntent);
		}

		playbackNotificationRouter
			.onReceive(
				ApplicationProvider.getApplicationContext(),
				new Intent(PlaylistEvents.onPlaylistPause));

		{
			final Intent playlistChangeIntent = new Intent(PlaylistEvents.onPlaylistChange);
			playlistChangeIntent.putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, 2);
			playbackNotificationRouter
				.onReceive(
					ApplicationProvider.getApplicationContext(),
					playlistChangeIntent);
		}

		playbackNotificationRouter
			.onReceive(
				ApplicationProvider.getApplicationContext(),
				new Intent(PlaylistEvents.onPlaylistStart));
	}

	@Test
	public void thenTheLoadingNotificationIsCalledCorrectly() {
		verify(notificationsController, times(2)).notifyForeground(loadingNotification, 43);
		verify(notificationsController, times(1)).notifyBackground(loadingNotification, 43);
	}

	@Test
	public void thenTheServiceIsStartedOnTheFirstServiceItem() {
		verify(notificationsController).notifyForeground(firstNotification, 43);
	}

	@Test
	public void thenTheNotificationIsSetToThePausedNotification() {
		verify(notificationsController, times(1)).notifyBackground(secondNotification, 43);
	}

	@Test
	public void thenTheServiceIsStartedOnTheSecondServiceItem() {
		verify(notificationsController, times(1)).notifyForeground(secondNotification, 43);
	}
}
