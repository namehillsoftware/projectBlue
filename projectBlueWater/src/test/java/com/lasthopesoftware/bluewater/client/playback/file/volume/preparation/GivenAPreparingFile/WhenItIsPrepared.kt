package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.GivenAPreparingFile

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.FakeFilePreparer
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumePreparer
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.AssertionsForClassTypes
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.ExecutionException

class WhenItIsPrepared {

	companion object {
		private val emptyPlaybackHandler = EmptyPlaybackHandler(0)
		private var returnedFile: PreparedPlayableFile? = null

		@JvmStatic
		@BeforeClass
		@Throws(ExecutionException::class, InterruptedException::class)
		fun establish() {
			val fakeFilePreparer = FakeFilePreparer(emptyPlaybackHandler, emptyPlaybackHandler)
			val maxFileVolumePreparer = MaxFileVolumePreparer(fakeFilePreparer) { sf: ServiceFile? -> Promise(.89f) }
			returnedFile = FuturePromise(maxFileVolumePreparer.promisePreparedPlaybackFile(
				ServiceFile(5),
				Duration.ZERO)).get()
		}
	}

	@Test
	fun thenTheFileIsReturned() {
		AssertionsForClassTypes.assertThat(returnedFile!!.playbackHandler).isEqualTo(emptyPlaybackHandler)
	}

	@Test
	fun thenTheVolumeIsManagedByTheMaxFileVolumeManager() {
		AssertionsForClassTypes.assertThat(returnedFile!!.playableFileVolumeManager.volume.toFuture().get()).isEqualTo(.89f)
	}
}
