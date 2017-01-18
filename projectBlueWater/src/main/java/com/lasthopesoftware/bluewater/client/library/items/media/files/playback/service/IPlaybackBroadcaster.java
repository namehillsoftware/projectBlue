package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;

/**
 * Created by david on 1/17/17.
 */
public interface IPlaybackBroadcaster {
    void sendPlaybackBroadcast(String broadcastMessage, PositionedPlaybackFile positionedPlaybackFile);
}
