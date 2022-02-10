package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.viewmodels.GivenAPlayingFile

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.viewmodels.NowPlayingViewModel
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlaying
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

private val nowPlayingViewModel by lazy {
	val nowPlayingRepository = mockk<INowPlayingRepository>()
	every { nowPlayingRepository.nowPlaying } returns Promise(
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

	val connectionProvider = mockk<ProvideSelectedConnection>()
	every { connectionProvider.promiseSessionConnection() } returns Promise(
		FakeConnectionProvider()
	)

	val nowPlayingViewModel = NowPlayingViewModel(
		mockk(),
		nowPlayingRepository,
		connectionProvider,
		mockk(),
		mockk(),
		mockk(),
		mockk(),
		mockk(),
		mockk(),
		mockk()
	)

	nowPlayingViewModel.initializeViewModel()

	nowPlayingViewModel
}

class WhenInitializingTheNowPlayingViewModel {

	@Test
	fun thenTheFilePositionIsCorrect() {
		assertThat(nowPlayingViewModel.filePosition.value).isEqualTo(853127)
	}
}
