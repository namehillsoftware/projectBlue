package com.lasthopesoftware.bluewater.client.playback.service.broadcasters;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;

public interface IPlaybackBroadcaster {
    void sendPlaybackBroadcast(String broadcastMessage, int libraryId, PositionedFile positionedFile);
}
