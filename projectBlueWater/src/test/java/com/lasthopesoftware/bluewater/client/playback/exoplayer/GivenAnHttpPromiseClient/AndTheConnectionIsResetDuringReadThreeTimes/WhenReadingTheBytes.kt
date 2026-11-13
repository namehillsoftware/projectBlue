package com.lasthopesoftware.bluewater.client.playback.exoplayer.GivenAnHttpPromiseClient.AndTheConnectionIsResetDuringReadThreeTimes

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
		private val retryingDataSource by lazy {
			val dataSpec = DataSpec(Uri.parse("http://mock-test"))
			var attempts = 0
			RetryingExoPlayerDataSource(
				mockk(relaxUnitFun = true) {
					every { open(match { it.position == 0L }) } answers {
						attempts++
						471
					}

					every { read(any(), any(), any()) } answers {
						if (attempts < 3) throw HttpDataSourceException.createForIOException(SocketException("Connection reset"), Util.castNonNull<DataSpec>(dataSpec), HttpDataSourceException.TYPE_READ)
						789
					}
				}
			)
		}
	}

	private var bytesRead: Int? = null
	private var exception: HttpDataSourceException? = null

	override fun before() {
		try {
			bytesRead = retryingDataSource.read(emptyByteArray, 999, 640)
		} catch (e: HttpDataSourceException) {
			exception = e
		}
	}

	@Test
	fun `then the bytes are not read`() {
		assertThat(bytesRead).isNull()
	}

	@Test
	fun `then the exception is thrown`() {
		assertThat(exception).isNotNull
	}
}
