package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.specs.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsStopped;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.LocalPlaybackBroadcaster;
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

	private static final CreateAndHold<Object> testSetup = new AbstractSynchronousLazy<Object>() {
		@Override
		protected Object create() {

			when(notificationContentBuilder.promiseNowPlayingNotification(any(), anyBoolean()))
				.thenReturn(new Promise<>(new Notification()));

			when(notificationContentBuilder.promiseNowPlayingNotification(argThat(a -> a.equals(new ServiceFile(2))), anyBoolean()))
				.thenReturn(new Promise<>(secondNotification));

			final PlaybackNotificationRouter playbackNotificationRouter =
				new PlaybackNotificationRouter(new PlaybackNotificationBroadcaster(
					service.getObject(),
					notificationManager,
					new PlaybackNotificationsConfiguration("",43, mediaSessionToken),
					notificationContentBuilder));

			final LocalPlaybackBroadcaster localPlaybackBroadcaster =
				new LocalPlaybackBroadcaster(RuntimeEnvironment.application);

			LocalBroadcastManager.getInstance(RuntimeEnvironment.application)
				.registerReceiver(
					playbackNotificationRouter,
					Stream.of(playbackNotificationRouter.registerForIntents())
						.reduce(new IntentFilter(), (intentFilter, action) -> {
							intentFilter.addAction(action);
							return intentFilter;
						}));

			localPlaybackBroadcaster.sendPlaybackBroadcast(
				PlaylistEvents.onPlaylistStart,
				1,
				new PositionedFile(1, new ServiceFile(1)));

			localPlaybackBroadcaster.sendPlaybackBroadcast(
				PlaylistEvents.onPlaylistChange,
				1,
				new PositionedFile(1, new ServiceFile(1)));

			localPlaybackBroadcaster.sendPlaybackBroadcast(
				PlaylistEvents.onPlaylistStop,
				1,
				new PositionedFile(1, new ServiceFile(1)));

			localPlaybackBroadcaster.sendPlaybackBroadcast(
				PlaylistEvents.onPlaylistChange,
				1,
				new PositionedFile(1, new ServiceFile(2)));

			return new Object();
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
