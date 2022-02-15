package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.viewmodels.GivenAPlayingFile

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeScopedCachedFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.StoreNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

private val nowPlayingViewModel by lazy {
	val nowPlayingRepository = mockk<MaintainNowPlayingState>().apply {
		every { promiseNowPlaying() } returns Promise(
			NowPlaying(
				listOf(
					ServiceFile(815),
					ServiceFile(449),
					ServiceFile(592),
					ServiceFile(355),
					ServiceFile(390),
				),
				3,
				439774,
				false
			)
		)
	}

	val connectionProvider = mockk<ProvideSelectedConnection>().apply {
		every { promiseSessionConnection() } returns Promise(FakeConnectionProvider())
	}

	val filePropertiesProvider = FakeScopedCachedFilesPropertiesProvider().apply {
		addFilePropertiesToCache(
			ServiceFile(355),
			emptyMap()
		)
	}

	val checkAuthentication = mockk<CheckIfScopedConnectionIsReadOnly>().apply {
		every { promiseIsReadOnly() } returns true.toPromise()
	}

	val playbackService = mockk<ControlPlaybackService>().apply {
		every { promiseIsMarkedForPlay() } returns true.toPromise()
	}

	val storeNowPlayingDisplaySettings = mockk<StoreNowPlayingDisplaySettings>().apply {
		every { isScreenOnDuringPlayback } returns true
	}

	val nowPlayingViewModel = NowPlayingFilePropertiesViewModel(
        mockk(relaxUnitFun = true),
        nowPlayingRepository,
        connectionProvider,
        filePropertiesProvider,
        mockk(),
        checkAuthentication,
        playbackService,
        mockk(),
        mockk(relaxed = true),
        storeNowPlayingDisplaySettings
    )

	nowPlayingViewModel.initializeViewModel()

	nowPlayingViewModel
}

class `When Initializing The NowPlayingViewModel` {

	@Test
	fun `then the file position is correct`() {
		assertThat(nowPlayingViewModel.filePosition.value).isEqualTo(439774)
	}
}
