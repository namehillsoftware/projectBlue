package com.lasthopesoftware.bluewater.client.playback.engine.preparation

import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource

interface ProvidePlayableFilePreparationSources {
    fun providePlayableFilePreparationSource(): PlayableFilePreparationSource
}
