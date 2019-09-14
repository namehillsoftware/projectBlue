package com.lasthopesoftware.bluewater.client.playback.service.broadcasters;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

public class PlaybackStartedBroadcaster {

	private final ISelectedLibraryIdentifierProvider libraryIdentifierProvider;
	private final IPlaybackBroadcaster playbackBroadcaster;

	public PlaybackStartedBroadcaster(ISelectedLibraryIdentifierProvider libraryIdentifierProvider, IPlaybackBroadcaster playbackBroadcaster) {
		this.libraryIdentifierProvider = libraryIdentifierProvider;
		this.playbackBroadcaster = playbackBroadcaster;
	}

	public void broadcastPlaybackStarted(PositionedFile positionedFile) {
		libraryIdentifierProvider.getSelectedLibraryId()
			.then(new VoidResponse<>(id -> playbackBroadcaster.sendPlaybackBroadcast(PlaylistEvents.onPlaylistStart, id, positionedFile)));
	}
}
