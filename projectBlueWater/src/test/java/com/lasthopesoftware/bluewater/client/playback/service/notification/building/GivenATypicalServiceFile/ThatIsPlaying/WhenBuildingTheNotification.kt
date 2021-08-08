package com.lasthopesoftware.bluewater.client.playback.service.notification.building.GivenATypicalServiceFile.ThatIsPlaying;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import androidx.core.app.NotificationCompat;
import androidx.test.core.app.ApplicationProvider;

import com.annimon.stream.Stream;
import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeFilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.connection.FakeFileConnectionProvider;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.NowPlayingNotificationBuilder;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class WhenBuildingTheNotification extends AndroidContext {

	private static final Lazy<Bitmap> expectedBitmap = new Lazy<>(() -> {
		final Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
		return Bitmap.createBitmap(1, 1, conf);
	});

	private static final NotificationCompat.Builder spiedBuilder = spy(new NotificationCompat.Builder(ApplicationProvider.getApplicationContext(), "test"));

	private static NotificationCompat.Builder builder;

	public void before() throws InterruptedException, ExecutionException {
		final FakeFileConnectionProvider connectionProvider = new FakeFileConnectionProvider();
		connectionProvider.setupFile(
			new ServiceFile(3),
			new HashMap<String, String>() {
				{
					put(KnownFileProperties.ARTIST, "test-artist");
					put(KnownFileProperties.NAME, "song");
				}
			});

		final IFilePropertiesContainerRepository containerRepository = new FakeFilePropertiesContainer();

		final ImageProvider imageProvider = mock(ImageProvider.class);
		when(imageProvider.promiseFileBitmap(any()))
			.thenReturn(new Promise<>(expectedBitmap.getObject()));

		final NowPlayingNotificationBuilder npBuilder = new NowPlayingNotificationBuilder(
			ApplicationProvider.getApplicationContext(),
			() -> spiedBuilder,
			connectionProvider,
			new ScopedCachedFilePropertiesProvider(
				connectionProvider,
				containerRepository,
				new ScopedFilePropertiesProvider(
					connectionProvider,
					containerRepository
				)),
			imageProvider);

		builder = new FuturePromise<>(npBuilder.promiseNowPlayingNotification(new ServiceFile(3), true)).get();
	}

	@Test
	public void thenTheNotificationHasAPauseButton() {
		assertThat(Stream.of(builder.mActions).map(a -> a.title).toList())
			.containsOnlyOnce(ApplicationProvider.getApplicationContext().getString(R.string.btn_pause));
	}

	@Test
	public void thenTheNotificationHasAPreviousButton() {
		assertThat(Stream.of(builder.mActions).map(a -> a.title).toList())
			.containsOnlyOnce(ApplicationProvider.getApplicationContext().getString(R.string.btn_previous));
	}

	@Test
	public void thenTheNotificationHasANextButton() {
		assertThat(Stream.of(builder.mActions).map(a -> a.title).toList())
			.containsOnlyOnce(ApplicationProvider.getApplicationContext().getString(R.string.btn_next));
	}

	@Test
	public void thenTheNotificationBitmapIsCorrect() {
		verify(spiedBuilder).setLargeIcon(expectedBitmap.getObject());
	}
}
