package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NowPlayingViewModel : ViewModel() {
	private var filePropertiesState = MutableStateFlow<MutableMap<String, String>?>(null)

	var fileProperties = filePropertiesState.asStateFlow()
	var promisedNowPlayingImage: Promise<Bitmap?>? = null
	var filePosition: Long = 0
	var fileDuration: Long = 0
	var isFilePropertiesReadOnly: Boolean? = null

	fun release() {
		promisedNowPlayingImage?.cancel()
	}
}
