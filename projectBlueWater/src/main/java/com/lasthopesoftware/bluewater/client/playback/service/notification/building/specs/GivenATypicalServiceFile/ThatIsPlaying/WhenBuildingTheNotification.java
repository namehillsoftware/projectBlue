package com.lasthopesoftware.bluewater.client.playback.service.notification.building.specs.GivenATypicalServiceFile.ThatIsPlaying;

import android.support.v4.app.NotificationCompat;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.specs.FakeFilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.library.sync.specs.FakeFileConnectionProvider;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.NowPlayingNotificationBuilder;
import com.lasthopesoftware.bluewater.shared.android.notifications.ProduceNotificationBuilders;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class WhenBuildingTheNotification {

	private NotificationCompat.Builder builder;

	@Before
	public void before() throws InterruptedException, ExecutionException {
		final ProduceNotificationBuilders notificationBuilders = mock(ProduceNotificationBuilders.class);
		when(notificationBuilders.getNotificationBuilder(any())).thenAnswer(a -> builder = new NotificationCompat.Builder(RuntimeEnvironment.application, a.getArgument(0)));

		final FakeFileConnectionProvider connectionProvider = new FakeFileConnectionProvider();
		connectionProvider.setupFile(
			new ServiceFile(3),
			new HashMap<String, String>() {
				{
					put(FilePropertiesProvider.ARTIST, "test-artist");
					put(FilePropertiesProvider.NAME, "song");
				}
			});

		final IFilePropertiesContainerRepository containerRepository = new FakeFilePropertiesContainer();

		final NowPlayingNotificationBuilder npBuilder = new NowPlayingNotificationBuilder(
			RuntimeEnvironment.application,
			() -> new NotificationCompat.Builder(RuntimeEnvironment.application, "test"),
			connectionProvider,
			new CachedFilePropertiesProvider(
				connectionProvider,
				containerRepository,
				new FilePropertiesProvider(
					connectionProvider,
					containerRepository)),
			mock(ImageProvider.class));

		new FuturePromise<>(npBuilder.promiseNowPlayingNotification(new ServiceFile(3), true)).get();
	}

	@Test
	public void thenTheNotificationHasAPlayingButton() {
		assertThat(Stream.of(builder.mActions).map(a -> a.title).toList())
			.containsOnlyOnce(RuntimeEnvironment.application.getString(R.string.btn_pause));
	}
}
