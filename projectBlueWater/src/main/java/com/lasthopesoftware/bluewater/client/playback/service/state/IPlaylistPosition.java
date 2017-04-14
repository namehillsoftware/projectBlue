package com.lasthopesoftware.bluewater.client.playback.service.state;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.promises.Promise;

import io.reactivex.Observable;

public interface IPlaylistPosition {
	Promise<PositionedFile> changePosition(final int playlistPosition, final int filePosition);
	Observable<PositionedPlaybackFile> observePosition();
}
