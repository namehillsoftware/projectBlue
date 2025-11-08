package com.lasthopesoftware.bluewater.features

import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.client.connection.http.HttpClientType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.exoplayer.HttpDataSourceType

@Keep
data class ApplicationFeatureConfiguration(
    val playbackEngineType: PlaybackEngineType? = null,
	val httpDataSourceType: HttpDataSourceType? = null,
	val httpClientType: HttpClientType? = null,
)
