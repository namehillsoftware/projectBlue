package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.broadcasters;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;

/**
 * Created by david on 1/17/17.
 */
public interface IPlaybackBroadcaster {
    void sendPlaybackBroadcast(String broadcastMessage, PositionedPlaybackFile positionedPlaybackFile);

    public static class PlaylistEvents {
        private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(PlaylistEvents.class);

        public static final String onPlaylistChange = magicPropertyBuilder.buildProperty("onPlaylistChange");
        public static final String onPlaylistStart = magicPropertyBuilder.buildProperty("onPlaylistStart");
        public static final String onPlaylistStop = magicPropertyBuilder.buildProperty("onPlaylistStop");
        public static final String onPlaylistPause = magicPropertyBuilder.buildProperty("onPlaylistPause");
        public static final String onFileComplete = magicPropertyBuilder.buildProperty("onFileComplete");

        public static class PlaybackFileParameters {
            private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(PlaybackFileParameters.class);

            public static final String fileKey = magicPropertyBuilder.buildProperty("fileKey");
            public static final String fileLibraryId = magicPropertyBuilder.buildProperty("fileLibraryId");
            public static final String filePosition = magicPropertyBuilder.buildProperty("filePosition");
            public static final String fileDuration = magicPropertyBuilder.buildProperty("fileDuration");
            public static final String isPlaying = magicPropertyBuilder.buildProperty("isPlaying");
        }

        public static class PlaylistParameters {
            public static final String playlistPosition = MagicPropertyBuilder.buildMagicPropertyName(PlaylistParameters.class, "playlistPosition");
        }
    }
}
