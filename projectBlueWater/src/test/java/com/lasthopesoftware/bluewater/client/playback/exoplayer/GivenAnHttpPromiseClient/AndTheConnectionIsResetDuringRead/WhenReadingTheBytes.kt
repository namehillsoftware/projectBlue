package com.lasthopesoftware.bluewater.client.playback.exoplayer.GivenAnHttpPromiseClient.AndTheConnectionIsResetDuringRead

import android.net.Uri
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource.HttpDataSourceException
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.exoplayer.RetryingExoPlayerDataSource
import com.lasthopesoftware.resources.emptyByteArray
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.SocketException

class WhenReadingTheBytes : AndroidContext() {
	companion object {
		private const val initialPosition = 553L

		private val retryingDataSource by lazy {
			val dataSpec = DataSpec(Uri.parse("http://test"))
			var isClosedFirst = false
			var isReopened = false

			RetryingExoPlayerDataSource(
				mockk {
					var position = 0L
					every { open(any()) } answers {
						position = firstArg<DataSpec>().position
						468
					}

					every { open(match { it.position == initialPosition + 587L }) } answers {
						position = firstArg<DataSpec>().position
						isReopened = isClosedFirst
						468
					}

					every { close() } answers {
						isClosedFirst = true
					}

					every { read(any(), 348, 790) } returns 587

					every { read(any(), 562, 643) } answers {
						if (!isReopened) throw HttpDataSourceException.createForIOException(SocketException("Connection reset"), Util.castNonNull<DataSpec>(dataSpec), HttpDataSourceException.TYPE_READ)
						670
					}
				}
			)
		}
	}

	private var bytesRead = 0

	override fun before() {
		retryingDataSource.open(DataSpec.Builder().setUri(Uri.parse("http://test")).setPosition(initialPosition).build())
		retryingDataSource.read(emptyByteArray, 348, 790)
		bytesRead = retryingDataSource.read(emptyByteArray, 562, 643)
	}

	@Test
	fun `then the bytes are read`() {
		assertThat(bytesRead).isEqualTo(670)
	}
}
