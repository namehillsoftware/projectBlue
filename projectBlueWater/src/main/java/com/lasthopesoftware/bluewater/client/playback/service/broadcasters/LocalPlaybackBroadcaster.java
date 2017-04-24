package com.lasthopesoftware.bluewater.client.playback.service.broadcasters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedFile;

/**
 * Created by david on 1/17/17.
 */

public class LocalPlaybackBroadcaster implements IPlaybackBroadcaster {
    private final LocalBroadcastManager localBroadcastManager;

    public LocalPlaybackBroadcaster(Context context) {
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public void sendPlaybackBroadcast(final String broadcastMessage, final int libraryId, final PositionedFile positionedFile) {
        final Intent playbackBroadcastIntent = new Intent(broadcastMessage);

        final int currentPlaylistPosition = positionedFile.getPlaylistPosition();

        final int fileKey = positionedFile.getKey();

        playbackBroadcastIntent
                .putExtra(PlaylistEvents.PlaylistParameters.playlistPosition, currentPlaylistPosition)
                .putExtra(PlaylistEvents.PlaybackFileParameters.fileLibraryId, libraryId)
                .putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, fileKey);


        localBroadcastManager.sendBroadcast(playbackBroadcastIntent);
    }
}
