package com.lasthopesoftware.bluewater.client.playback.file.initialization

import android.net.Uri
import java.io.IOException

interface IPlaybackInitialization<TMediaPlayer> {
    @Throws(IOException::class)
    fun initializeMediaPlayer(fileUri: Uri?): TMediaPlayer
}
