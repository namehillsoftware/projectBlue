package com.lasthopesoftware.bluewater.client.playback.exoplayer.GivenNullDataSourceIsSelected

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ServerHttpDataSourceProvider
import com.lasthopesoftware.bluewater.features.ApplicationFeatureConfiguration
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When getting the http data source provider` {

	private val mut by lazy {
		ServerHttpDataSourceProvider<Any>(
			mockk(),
			mockk {
				every { getStreamingOkHttpClient(any()) } returns mockk()
			},
			mockk {
				every { promiseFeatureConfiguration() } returns ApplicationFeatureConfiguration(
					httpDataSourceType = null
				).toPromise()
			}
		)
	}

	private var dataSource: DataSource.Factory? = null

	@BeforeAll
	fun act() {
		dataSource = mut.promiseDataSourceFactory(Any()).toExpiringFuture().get()
	}

	@Test
	fun `then the datasource is correct`() {
		assertThat(dataSource).isInstanceOf(cls<OkHttpDataSource.Factory>())
	}
}
