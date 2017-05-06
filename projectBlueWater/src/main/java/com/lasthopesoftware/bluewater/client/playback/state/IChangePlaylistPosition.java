package com.lasthopesoftware.bluewater.client.playback.state;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.promises.Promise;

import java.io.IOException;

public interface IChangePlaylistPosition {
	Promise<PositionedFile> changePosition(final int playlistPosition, final int filePosition) throws IOException;
}
