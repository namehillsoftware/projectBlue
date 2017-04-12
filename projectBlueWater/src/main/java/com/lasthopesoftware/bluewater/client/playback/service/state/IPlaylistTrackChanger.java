package com.lasthopesoftware.bluewater.client.playback.service.state;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedFile;
import com.lasthopesoftware.promises.Promise;

public interface IPlaylistTrackChanger {
	Promise<PositionedFile> changePosition(final int playlistPosition, final int filePosition);
}
