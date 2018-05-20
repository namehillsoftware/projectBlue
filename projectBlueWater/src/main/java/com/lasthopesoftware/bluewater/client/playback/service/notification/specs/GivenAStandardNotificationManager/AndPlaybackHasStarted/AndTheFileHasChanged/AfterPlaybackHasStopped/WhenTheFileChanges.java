package com.lasthopesoftware.bluewater.client.playback.service.notification.specs.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AfterPlaybackHasStopped;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.notification.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.shared.promises.extensions.PromiseMessenger;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class WhenTheFileChanges {

	private static final Notification secondNotification = new Notification();
	private static final CreateAndHold<Service> service = new Lazy<>(() -> spy(Robolectric.buildService(PlaybackService.class).get()));
	private static final NotificationManager notificationManager = mock(NotificationManager.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	private static final CreateAndHold<Void> testSetup = new AbstractSynchronousLazy<Void>() {
		@Override
		protected Void create() {

			when(notificationContentBuilder.promiseNowPlayingNotification(any(), anyBoolean()))
				.thenReturn(new Promise<>(new Notification()));

			final PromiseMessenger<Notification> secondNotificationPromise = new PromiseMessenger<>();
			when(notificationContentBuilder.promiseNowPlayingNotification(argThat(a -> a.equals(new ServiceFile(2))), anyBoolean()))
				.thenReturn(secondNotificationPromise);

			final PlaybackNotificationBroadcaster playbackNotificationBroadcaster =
				new PlaybackNotificationBroadcaster(
					service.getObject(),
					notificationManager,
					new PlaybackNotificationsConfiguration(43),
					notificationContentBuilder);

			playbackNotificationBroadcaster.notifyPlaying();
			playbackNotificationBroadcaster.notifyPlayingFileChanged(new ServiceFile(1));
			playbackNotificationBroadcaster.notifyPlayingFileChanged(new ServiceFile(2));
			playbackNotificationBroadcaster.notifyStopped();
			secondNotificationPromise.sendResolution(secondNotification);

			return null;
		}
	};

	@Before
	public void context() {
		testSetup.getObject();
	}

	@Test
	public void thenTheServiceIsStartedInTheForegroundOnce() {
		verify(service.getObject(), times(1))
			.startForeground(eq(43), any());
	}

	@Test
	public void thenTheServiceDoesNotContinueInTheBackground() {
		verify(service.getObject()).stopForeground(true);
	}

	@Test
	public void thenTheNotificationIsNotSetToTheSecondNotification() {
		verify(notificationManager, never()).notify(43, secondNotification);
	}
}
