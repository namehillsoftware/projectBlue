package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.GivenATypicalServiceFile.ThatIsPaused

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeRevisionProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.NowPlayingNotificationBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.bitmaps.ImmediateBitmapProducer
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenBuildingTheLoadingNotification : AndroidContext() {

	companion object {
		private val expectedBitmap by lazy {
			byteArrayOf(170.toByte(), 772.toByte(), 408.toByte(), 730.toByte())
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
		val connectionProvider = FakeConnectionProvider()
		connectionProvider.setupFile(
			ServiceFile(3),
			object : HashMap<String, String>() {
				init {
					put(KnownFileProperties.Artist, "test-artist")
					put(KnownFileProperties.Name, "song")
				}
			})
		val containerRepository = FakeFilePropertiesContainerRepository()

		val libraryId = LibraryId(605)
		val fakeLibraryConnectionProvider = FakeLibraryConnectionProvider(mapOf(Pair(libraryId, connectionProvider)))
		val npBuilder = NowPlayingNotificationBuilder(
			ApplicationProvider.getApplicationContext(),
			mockk {
				every { getMediaStyleNotification(libraryId) } returns spiedBuilder
			},
			UrlKeyProvider(fakeLibraryConnectionProvider),
            FilePropertiesProvider(
				GuaranteedLibraryConnectionProvider(fakeLibraryConnectionProvider),
				FakeRevisionProvider(1),
				containerRepository,
			),
			mockk {
				every { promiseImageBytes(libraryId, any<ServiceFile>()) } returns Promise(expectedBitmap)
			},
			ImmediateBitmapProducer,
		)
		builder = npBuilder.promiseLoadingNotification(libraryId, false).toExpiringFuture().get()
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
