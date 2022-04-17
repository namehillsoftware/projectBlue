package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.GivenAPreparingFile

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.FakeFilePreparer
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumePreparer
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test

class WhenItIsPrepared {

	companion object {
		private val emptyPlaybackHandler = EmptyPlaybackHandler(0)
		private var returnedFile: PreparedPlayableFile? = null

		@JvmStatic
		@BeforeClass
		fun establish() {
			val fakeFilePreparer = FakeFilePreparer(emptyPlaybackHandler, emptyPlaybackHandler)
			val maxFileVolumePreparer = MaxFileVolumePreparer(fakeFilePreparer) { Promise(.89f) }
			returnedFile = ExpiringFuturePromise(maxFileVolumePreparer.promisePreparedPlaybackFile(
				ServiceFile(5),
				Duration.ZERO)).get()
		}
	}

	@Test
	fun thenTheFileIsReturned() {
		assertThat(returnedFile!!.playbackHandler).isEqualTo(emptyPlaybackHandler)
	}

	@Test
	fun thenTheVolumeIsManagedByTheMaxFileVolumeManager() {
		assertThat(returnedFile!!.playableFileVolumeManager.volume.toExpiringFuture().get()).isEqualTo(.89f)
	}
}
