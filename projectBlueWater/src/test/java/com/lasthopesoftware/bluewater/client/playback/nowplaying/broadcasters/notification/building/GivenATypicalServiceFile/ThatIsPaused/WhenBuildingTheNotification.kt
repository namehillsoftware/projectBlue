package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.GivenATypicalServiceFile.ThatIsPaused

import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeRevisionProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeFileConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
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
		private val expectedBitmap by lazy {
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
				Pair(KnownFileProperties.Name, "song")
			)
		)
		val libraryId = LibraryId(803)
		val libraryConnectionProvider = FakeLibraryConnectionProvider(mapOf(
			Pair(libraryId, connectionProvider)
		))
		val containerRepository = FakeFilePropertiesContainerRepository()

		val npBuilder = NowPlayingNotificationBuilder(
			ApplicationProvider.getApplicationContext(),
			mockk {
				every { getMediaStyleNotification(libraryId) } returns spiedBuilder
			},
			UrlKeyProvider(libraryConnectionProvider),
            CachedFilePropertiesProvider(
				libraryConnectionProvider,
				containerRepository,
				FilePropertiesProvider(
					libraryConnectionProvider,
					FakeRevisionProvider(1),
					containerRepository
				)
			),
			mockk {
				every { promiseFileBitmap(any()) } returns Promise(expectedBitmap)
			}
		)
		builder =
			ExpiringFuturePromise(npBuilder.promiseNowPlayingNotification(libraryId, ServiceFile(3), false)).get()
	}

	@Test
	fun thenTheNotificationHasAPlayButton() {
		assertThat(builder!!.mActions.map { a -> a.title }).containsOnlyOnce(ApplicationProvider.getApplicationContext<Context>().getString(R.string.btn_play))
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
		verify { spiedBuilder.setLargeIcon(expectedBitmap) }
	}
}
