package com.lasthopesoftware.bluewater.client.playback.service.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted;

import android.app.Notification;

import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;

import static com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.newFakeBuilder;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenTheFileChanges extends AndroidContext {

	private static final Notification loadingNotification = new Notification();
	private static final Notification startedNotification = new Notification();
	private static final ControlNotifications notificationController = mock(ControlNotifications.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	@Override
	public void before() {
		when(notificationContentBuilder.getLoadingNotification(anyBoolean()))
			.thenReturn(newFakeBuilder(loadingNotification));

		when(notificationContentBuilder.promiseNowPlayingNotification(new ServiceFile(1), true))
			.thenReturn(new Promise<>(newFakeBuilder(startedNotification)));

		final PlaybackNotificationBroadcaster playbackNotificationBroadcaster =
			new PlaybackNotificationBroadcaster(
				notificationController,
				new NotificationsConfiguration("", 43),
				notificationContentBuilder,
				() -> new Promise<>(newFakeBuilder(new Notification())));

		playbackNotificationBroadcaster.notifyPlaying();
		playbackNotificationBroadcaster.notifyPlayingFileChanged(new ServiceFile(1));
	}

	@Test
	public void thenTheLoadingNotificationIsStarted() {
		verify(notificationController).notifyForeground(loadingNotification, 43);
	}

	@Test
	public void thenTheServiceIsStartedInTheForeground() {
		verify(notificationController).notifyForeground(startedNotification, 43);
	}
}
