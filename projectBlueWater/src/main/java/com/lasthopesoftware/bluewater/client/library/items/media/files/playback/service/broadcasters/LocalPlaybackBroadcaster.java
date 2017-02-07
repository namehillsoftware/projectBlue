package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.broadcasters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.vedsoft.futures.callables.VoidFunc;

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
            .getActiveLibrary(context)
            .then(VoidFunc.runningCarelessly(library -> {
                final Intent playbackBroadcastIntent = new Intent(broadcastMessage);

                final int currentPlaylistPosition = positionedPlaybackFile.getPosition();

                final int fileKey = positionedPlaybackFile.getKey();

				final IPlaybackHandler playbackHandler = positionedPlaybackFile.getPlaybackHandler();

                playbackBroadcastIntent
                        .putExtra(IPlaybackBroadcaster.PlaylistEvents.PlaylistParameters.playlistPosition, currentPlaylistPosition)
                        .putExtra(IPlaybackBroadcaster.PlaylistEvents.PlaybackFileParameters.fileLibraryId, library.getId())
                        .putExtra(IPlaybackBroadcaster.PlaylistEvents.PlaybackFileParameters.fileKey, fileKey)
                        .putExtra(IPlaybackBroadcaster.PlaylistEvents.PlaybackFileParameters.filePosition, playbackHandler.getCurrentPosition())
                        .putExtra(IPlaybackBroadcaster.PlaylistEvents.PlaybackFileParameters.isPlaying, playbackHandler.isPlaying());

                if (playbackHandler.getDuration() > 0) {
					playbackBroadcastIntent.putExtra(PlaylistEvents.PlaybackFileParameters.fileDuration, playbackHandler.getDuration());

					localBroadcastManager.sendBroadcast(playbackBroadcastIntent);
					return;
				}

                final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(SessionConnection.getSessionConnectionProvider(), fileKey);
                filePropertiesProvider.onComplete(fileProperties -> {
                    playbackBroadcastIntent
                            .putExtra(IPlaybackBroadcaster.PlaylistEvents.PlaybackFileParameters.fileDuration, FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties));

                    localBroadcastManager.sendBroadcast(playbackBroadcastIntent);
                }).onError(error -> {
                    playbackBroadcastIntent
                            .putExtra(IPlaybackBroadcaster.PlaylistEvents.PlaybackFileParameters.fileDuration, -1);

                    localBroadcastManager.sendBroadcast(playbackBroadcastIntent);
                    return true;
                }).execute();
            }));
    }
}
