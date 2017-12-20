package com.lasthopesoftware.bluewater.client.playback.engine;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.IOException;

public interface IChangePlaylistPosition {
	Promise<PositionedFile> changePosition(final int playlistPosition, final int filePosition) throws IOException;
}
