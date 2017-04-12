package com.lasthopesoftware.bluewater.client.playback.service.state;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.promises.Promise;

import io.reactivex.ObservableOnSubscribe;

/**
 * Created by david on 4/9/17.
 */

public interface IStartedPlaylist extends IPlaylistTrackChanger, ObservableOnSubscribe<PositionedPlaybackFile> {
	Promise<IPausedPlaylist> pause();
	IStartedPlaylist playRepeatedly();
	IStartedPlaylist playToCompletion();
	void setVolume(float volume);
}
