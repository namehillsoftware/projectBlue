package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.broadcasters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.IPlaybackBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.lazyj.Lazy;

/**
 * Created by david on 1/17/17.
 */

public class LocalPlaybackBroadcaster implements IPlaybackBroadcaster {
    private final Context context;
    private final LocalBroadcastManager localBroadcastManager;

    public LocalPlaybackBroadcaster(Context context) {
        this.context = context;
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public void sendPlaybackBroadcast(final String broadcastMessage, final PositionedPlaybackFile positionedPlaybackFile) {
        LibrarySession
            .GetActiveLibrary(context)
            .then(VoidFunc.running(library -> {
                final Intent playbackBroadcastIntent = new Intent(broadcastMessage);

                final int currentPlaylistPosition = positionedPlaybackFile.getPosition();

                final int fileKey = positionedPlaybackFile.getKey();

                playbackBroadcastIntent
                        .putExtra(PlaybackService.PlaylistEvents.PlaylistParameters.playlistPosition, currentPlaylistPosition)
                        .putExtra(PlaybackService.PlaylistEvents.PlaybackFileParameters.fileLibraryId, library.getId())
                        .putExtra(PlaybackService.PlaylistEvents.PlaybackFileParameters.fileKey, fileKey)
                        .putExtra(PlaybackService.PlaylistEvents.PlaybackFileParameters.filePosition, currentPlaylistPosition)
                        .putExtra(PlaybackService.PlaylistEvents.PlaybackFileParameters.isPlaying, positionedPlaybackFile.getPlaybackHandler().isPlaying());

                final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(SessionConnection.getSessionConnectionProvider(), fileKey);
                filePropertiesProvider.onComplete(fileProperties -> {
                    playbackBroadcastIntent
                            .putExtra(PlaybackService.PlaylistEvents.PlaybackFileParameters.fileDuration, FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties));

                    localBroadcastManager.sendBroadcast(playbackBroadcastIntent);
                }).onError(error -> {
                    playbackBroadcastIntent
                            .putExtra(PlaybackService.PlaylistEvents.PlaybackFileParameters.fileDuration, -1);

                    localBroadcastManager.sendBroadcast(playbackBroadcastIntent);
                    return true;
                }).execute();
            }));
    }
}
