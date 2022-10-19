package com.lasthopesoftware.bluewater.client.playback.service.notification.building.GivenATypicalServiceFile.ThatIsPlaying

import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeFileConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ScopedUrlKeyProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.NowPlayingNotificationBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenBuildingTheNotification : AndroidContext() {

	companion object {
		private val expectedBitmap = lazy {
			val conf = Bitmap.Config.ARGB_8888 // see other conf types
			Bitmap.createBitmap(1, 1, conf)
		}
		private val spiedBuilder = spyk(
			NotificationCompat.Builder(
				ApplicationProvider.getApplicationContext(),
				"test"
			)
		)
		private var builder: NotificationCompat.Builder? = null
	}

	override fun before() {
		val connectionProvider = FakeFileConnectionProvider()
		connectionProvider.setupFile(
			ServiceFile(3),
			mapOf(
				Pair(KnownFileProperties.Artist, "test-artist"),
				Pair(KnownFileProperties.Name, "song")))
		val containerRepository: IFilePropertiesContainerRepository = FakeFilePropertiesContainerRepository()
		val imageProvider = mockk<ProvideImages>()
		every { imageProvider.promiseFileBitmap(any()) } returns Promise(expectedBitmap.value)
		val npBuilder = NowPlayingNotificationBuilder(
			ApplicationProvider.getApplicationContext(),
			{ spiedBuilder },
			ScopedUrlKeyProvider(connectionProvider),
			ScopedCachedFilePropertiesProvider(
				connectionProvider,
				containerRepository,
				ScopedFilePropertiesProvider(
					connectionProvider,
					FakeScopedRevisionProvider(1),
					containerRepository
				)
			),
			imageProvider
		)
		builder = ExpiringFuturePromise(npBuilder.promiseNowPlayingNotification(ServiceFile(3), true)).get()
	}

	@Test
	fun thenTheNotificationHasAPauseButton() {
		assertThat(builder!!.mActions.map { a -> a.title }).containsOnlyOnce(ApplicationProvider.getApplicationContext<Context>().getString(R.string.btn_pause))
	}

	@Test
	fun thenTheNotificationHasAPreviousButton() {
		assertThat(builder!!.mActions.map { a -> a.title }).containsOnlyOnce(ApplicationProvider.getApplicationContext<Context>()
					.getString(R.string.btn_previous))
	}

	@Test
	fun thenTheNotificationHasANextButton() {
		assertThat(builder!!.mActions.map { a -> a.title }).containsOnlyOnce(ApplicationProvider.getApplicationContext<Context>().getString(R.string.btn_next))
	}

	@Test
	fun thenTheNotificationBitmapIsCorrect() {
		verify { spiedBuilder.setLargeIcon(expectedBitmap.value) }
	}
}
