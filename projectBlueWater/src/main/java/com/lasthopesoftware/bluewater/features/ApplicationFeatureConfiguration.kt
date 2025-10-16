package com.lasthopesoftware.bluewater.features

import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType

@Keep
data class ApplicationFeatureConfiguration(
    val playbackEngineType: PlaybackEngineType? = null,
)
