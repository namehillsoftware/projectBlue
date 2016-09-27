package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization;

/**
 * Created by david on 9/24/16.
 */

public interface IPlaybackInitialization<TMediaPlayer> {
	TMediaPlayer initializeMediaPlayer(String fileUri);
}
