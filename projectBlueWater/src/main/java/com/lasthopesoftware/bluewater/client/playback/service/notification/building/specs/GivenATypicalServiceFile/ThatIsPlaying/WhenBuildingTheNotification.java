package com.lasthopesoftware.bluewater.client.playback.service.notification.building.specs.GivenATypicalServiceFile.ThatIsPlaying;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.NowPlayingNotificationBuilder;
import com.lasthopesoftware.bluewater.shared.android.notifications.ProduceNotificationBuilders;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class WhenBuildingTheNotification {

	private NotificationCompat.Builder builder;

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@Before
	public void before() throws InterruptedException {
		final ProduceNotificationBuilders notificationBuilders = mock(ProduceNotificationBuilders.class);
		when(notificationBuilders.getNotificationBuilder(any())).thenAnswer(a -> builder = new NotificationCompat.Builder(RuntimeEnvironment.application, a.getArgument(0)));

		final IConnectionProvider connectionProvider = new FakeConnectionProvider();

		final MediaSessionCompat mediaSessionCompat = new MediaSessionCompat(RuntimeEnvironment.application, "test");
		final NowPlayingNotificationBuilder npBuilder = new NowPlayingNotificationBuilder(
			RuntimeEnvironment.application,
			notificationBuilders,
			connectionProvider,
			new CachedFilePropertiesProvider(
				connectionProvider,
				FilePropertyCache.getInstance(),
				new FilePropertiesProvider(
					connectionProvider,
					FilePropertyCache.getInstance())),
			mock(ImageProvider.class),
			new PlaybackNotificationsConfiguration("channel", 1, mediaSessionCompat.getSessionToken()));

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		npBuilder.promiseNowPlayingNotification(new ServiceFile(3), true)
			.then(n -> {
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenTheNotificationHasAPlayingButton() {
		assertThat(Stream.of(builder.mActions).map(a -> a.title).toList())
			.containsOnlyOnce(RuntimeEnvironment.application.getString(R.string.btn_pause));
	}
}
