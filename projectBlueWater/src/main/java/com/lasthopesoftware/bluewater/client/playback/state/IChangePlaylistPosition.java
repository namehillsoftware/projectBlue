package com.lasthopesoftware.bluewater.client.playback.state;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedFile;
import com.lasthopesoftware.promises.Promise;

public interface IChangePlaylistPosition {
	Promise<PositionedFile> changePosition(final int playlistPosition, final int filePosition);
}
