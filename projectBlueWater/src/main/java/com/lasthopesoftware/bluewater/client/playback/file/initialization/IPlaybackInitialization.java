package com.lasthopesoftware.bluewater.client.playback.file.initialization;

import android.net.Uri;

import java.io.IOException;

/**
 * Created by david on 9/24/16.
 */

public interface IPlaybackInitialization<TMediaPlayer> {
	TMediaPlayer initializeMediaPlayer(Uri fileUri) throws IOException;
}
