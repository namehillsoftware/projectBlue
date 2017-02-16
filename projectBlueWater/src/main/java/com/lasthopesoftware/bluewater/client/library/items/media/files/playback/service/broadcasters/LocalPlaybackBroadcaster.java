package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.broadcasters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;

/**
 * Created by david on 1/17/17.
 */

public class LocalPlaybackBroadcaster implements IPlaybackBroadcaster {
    private final LocalBroadcastManager localBroadcastManager;

    public LocalPlaybackBroadcaster(Context context) {
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public void sendPlaybackBroadcast(final String broadcastMessage, final int libraryId, final PositionedPlaybackFile positionedPlaybackFile) {
        final Intent playbackBroadcastIntent = new Intent(broadcastMessage);

        final int currentPlaylistPosition = positionedPlaybackFile.getPosition();

        final int fileKey = positionedPlaybackFile.getKey();

        final IPlaybackHandler playbackHandler = positionedPlaybackFile.getPlaybackHandler();

        playbackBroadcastIntent
                .putExtra(PlaylistEvents.PlaylistParameters.playlistPosition, currentPlaylistPosition)
                .putExtra(PlaylistEvents.PlaybackFileParameters.fileLibraryId, libraryId)
                .putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, fileKey)
                .putExtra(PlaylistEvents.PlaybackFileParameters.isPlaying, playbackHandler.isPlaying());


        localBroadcastManager.sendBroadcast(playbackBroadcastIntent);
    }
}
