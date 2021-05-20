package com.lasthopesoftware.bluewater.client.playback.service.broadcasters;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;

public interface IPlaybackBroadcaster {
    void sendPlaybackBroadcast(String broadcastMessage, LibraryId libraryId, PositionedFile positionedFile);
}
