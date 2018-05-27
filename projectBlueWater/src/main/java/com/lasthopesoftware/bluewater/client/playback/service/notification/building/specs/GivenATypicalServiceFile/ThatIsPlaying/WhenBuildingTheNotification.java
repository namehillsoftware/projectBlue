package com.lasthopesoftware.bluewater.client.playback.service.notification.building.specs.GivenATypicalServiceFile.ThatIsPlaying;

import android.graphics.Bitmap;
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
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.bluewater.shared.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenBuildingTheNotification extends AndroidContext {

	private static NotificationCompat.Builder builder;

	public void before() throws InterruptedException, ExecutionException {
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


		final Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
		final Bitmap bmp = Bitmap.createBitmap(1, 1, conf);
		final ImageProvider imageProvider = mock(ImageProvider.class);
		when(imageProvider.promiseFileBitmap(any()))
			.thenReturn(new Promise<>(bmp));

		final NowPlayingNotificationBuilder npBuilder = new NowPlayingNotificationBuilder(
			RuntimeEnvironment.application,
			() -> builder = new NotificationCompat.Builder(RuntimeEnvironment.application, "test"),
			connectionProvider,
			new CachedFilePropertiesProvider(
				connectionProvider,
				containerRepository,
				new FilePropertiesProvider(
					connectionProvider,
					containerRepository)),
			imageProvider);

		new FuturePromise<>(npBuilder.promiseNowPlayingNotification(new ServiceFile(3), true)).get();
	}

	@Test
	public void thenTheNotificationHasAPlayingButton() {
		assertThat(Stream.of(builder.mActions).map(a -> a.title).toList())
			.containsOnlyOnce(RuntimeEnvironment.application.getString(R.string.btn_pause));
	}

	@Test
	public void thenTheNotificationHasAPreviousButton() {
		assertThat(Stream.of(builder.mActions).map(a -> a.title).toList())
			.containsOnlyOnce(RuntimeEnvironment.application.getString(R.string.btn_previous));
	}
}
