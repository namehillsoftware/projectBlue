package com.lasthopesoftware.bluewater.client.playback.service.broadcasters;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class PlaybackStartedBroadcaster implements ImmediateResponse<Observable<PositionedPlaybackFile>, Observable<PositionedPlaybackFile>>, Consumer<PositionedPlaybackFile> {

	private final ISelectedLibraryIdentifierProvider libraryIdentifierProvider;
	private final IPlaybackBroadcaster playbackBroadcaster;
	private Disposable subscription;

	public PlaybackStartedBroadcaster(ISelectedLibraryIdentifierProvider libraryIdentifierProvider, IPlaybackBroadcaster playbackBroadcaster) {
		this.libraryIdentifierProvider = libraryIdentifierProvider;
		this.playbackBroadcaster = playbackBroadcaster;
	}

	@Override
	public Observable<PositionedPlaybackFile> respond(Observable<PositionedPlaybackFile> observable) {
		if (subscription != null)
			subscription.dispose();

		subscription = observable.firstElement().subscribe(this);

		return observable;
	}

	@Override
	public void accept(PositionedPlaybackFile p) throws Exception {
		playbackBroadcaster.sendPlaybackBroadcast(PlaylistEvents.onPlaylistStart, libraryIdentifierProvider.getSelectedLibraryId(), p.asPositionedFile());
	}
}
