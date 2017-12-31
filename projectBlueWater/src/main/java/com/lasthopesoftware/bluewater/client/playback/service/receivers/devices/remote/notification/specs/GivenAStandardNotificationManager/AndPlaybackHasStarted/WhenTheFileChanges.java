package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.notification.specs.GivenAStandardNotificationManager.AndPlaybackHasStarted;

import android.app.NotificationManager;
import android.app.Service;
import android.support.v4.app.NotificationCompat;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.notification.NotificationBroadcaster;
import com.lasthopesoftware.resources.intents.IntentFactory;
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

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class WhenTheFileChanges {

	private static final CreateAndHold<Service> service = new Lazy<>(() -> spy(Robolectric.buildService(PlaybackService.class).get()));
	private static final NotificationManager notificationManager = mock(NotificationManager.class);
	private static final NotificationCompat.Builder builder = mock(NotificationCompat.Builder.class);

	private static final CreateAndHold<Void> testSetup = new AbstractSynchronousLazy<Void>() {
		@Override
		protected Void create() throws Throwable {
			final CachedFilePropertiesProvider cachedFilePropertiesProvider =
				new CachedFilePropertiesProvider(
					new FakeConnectionProvider(),
					mock(IFilePropertiesContainerRepository.class),
					fileKey -> new Promise<>(new HashMap<>()));

			final NotificationBroadcaster notificationBroadcaster =
				new NotificationBroadcaster(
					service.getObject(),
					cachedFilePropertiesProvider,
					notificationManager,
					() -> builder,
					new PlaybackNotificationsConfiguration(43),
					new IntentFactory(RuntimeEnvironment.application));

			notificationBroadcaster.setPlaying();
			notificationBroadcaster.updateNowPlaying(new ServiceFile(1));

			return null;
		}
	};

	@Before
	public void context() {
		testSetup.getObject();
	}

	@Test
	public void thenTheServiceIsStartedInTheForeground() {
		verify(service.getObject()).startForeground(eq(43), any());
	}

	@Test
	public void thenTheNotificationIsOngoing() {
		verify(builder).setOngoing(true);
	}
}
