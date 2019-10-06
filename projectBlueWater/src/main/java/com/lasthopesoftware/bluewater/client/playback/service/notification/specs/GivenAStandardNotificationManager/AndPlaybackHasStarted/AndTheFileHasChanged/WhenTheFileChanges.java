package com.lasthopesoftware.bluewater.client.playback.service.notification.specs.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.resources.notifications.control.NotificationsController;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Test;
import org.robolectric.Robolectric;

import static com.lasthopesoftware.resources.notifications.specs.FakeNotificationCompatBuilder.newFakeBuilder;
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
		when(notificationContentBuilder.promiseNowPlayingNotification(any(), anyBoolean()))
			.thenReturn(new Promise<>(newFakeBuilder(new Notification())));

		when(notificationContentBuilder.promiseNowPlayingNotification(new ServiceFile(2), true))
			.thenReturn(new Promise<>(newFakeBuilder(nextNotification)));

		final PlaybackNotificationBroadcaster playbackNotificationBroadcaster =
			new PlaybackNotificationBroadcaster(
				new NotificationsController(
					service.getObject(),
					notificationManager),
				new PlaybackNotificationsConfiguration("",43),
				notificationContentBuilder);

		playbackNotificationBroadcaster.notifyPlaying();
		playbackNotificationBroadcaster.notifyPlayingFileChanged(new ServiceFile(1));
	}

	@Test
	public void thenTheServiceIsStartedInTheForeground() {
		verify(service.getObject()).startForeground(43, startedNotification);
	}
}
