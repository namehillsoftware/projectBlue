package com.lasthopesoftware.bluewater.client.playback.service.notification.building.GivenATypicalServiceFile.ThatIsPaused

import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ImageProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeFileConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.NowPlayingNotificationBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*

class WhenBuildingTheLoadingNotification : AndroidContext() {

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
			object : HashMap<String?, String?>() {
				init {
					put(KnownFileProperties.ARTIST, "test-artist")
					put(KnownFileProperties.NAME, "song")
				}
			})
		val containerRepository = FakeFilePropertiesContainer()
		val imageProvider = mockk<ImageProvider>()
		every { imageProvider.promiseFileBitmap(any()) } returns Promise(expectedBitmap.value)

		val npBuilder = NowPlayingNotificationBuilder(
			ApplicationProvider.getApplicationContext(),
			{ spiedBuilder },
			connectionProvider,
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
		builder = npBuilder.getLoadingNotification(false)
	}

	@Test
	fun thenTheNotificationHasAPlayButton() {
		assertThat(builder!!.mActions.map { a -> a.title })
			.containsOnlyOnce(ApplicationProvider.getApplicationContext<Context>().getString(R.string.btn_play))
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
		verify { spiedBuilder.setContentTitle(ApplicationProvider.getApplicationContext<Context>().getString(R.string.lbl_loading)) }
	}
}
