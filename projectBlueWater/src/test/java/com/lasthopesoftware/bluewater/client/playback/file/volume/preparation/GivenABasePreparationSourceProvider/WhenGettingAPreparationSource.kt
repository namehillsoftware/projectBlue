package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.GivenABasePreparationSourceProvider

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumePreparationProvider
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumePreparer
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingAPreparationSource {
    private val playableFileSource by lazy {
        val maxFileVolumePreparationProvider =
            MaxFileVolumePreparationProvider(object : IPlayableFilePreparationSourceProvider {
                override fun providePlayableFilePreparationSource(): PlayableFilePreparationSource {
                    return PlayableFilePreparationSource { _, _, _ ->
                        Promise(PreparedPlayableFile(
                            EmptyPlaybackHandler(0),
                            mockk(),
                            object : IBufferingPlaybackFile {
                                override fun promiseBufferedPlaybackFile(): Promise<IBufferingPlaybackFile> {
                                    return Promise<IBufferingPlaybackFile>(this)
                                }
                            }
                        ))
                    }
                }

                override val maxQueueSize: Int
                    get() {
                        return 13
                    }
            }, mockk())
        maxFileVolumePreparationProvider.providePlayableFilePreparationSource()
    }

    @Test
    fun `then the playable file source is a max volume preparer`() {
        assertThat(playableFileSource).isInstanceOf(MaxFileVolumePreparer::class.java)
    }
}
