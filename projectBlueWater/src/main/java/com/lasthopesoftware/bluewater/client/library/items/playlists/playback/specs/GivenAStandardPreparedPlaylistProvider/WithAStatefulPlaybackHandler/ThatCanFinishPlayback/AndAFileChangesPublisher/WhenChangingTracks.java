package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.AndAFileChangesPublisher;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.StatefulPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayer;
import com.lasthopesoftware.promises.ExpectedPromise;
import com.lasthopesoftware.promises.IPromise;

import org.junit.Before;
import org.junit.Test;

import rx.subjects.PublishSubject;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 12/17/16.
 */

public class WhenChangingTracks {

	private PositionedPlaybackFile positionedPlaybackFile;
	private PositionedPlaybackFile expectedPositionedPlaybackFile;

	@Before
	public void context() {
		final ResolveablePlaybackHandler playbackHandler = new ResolveablePlaybackHandler();
		final StatefulPlaybackHandler playbackHandlerUnderTest = new StatefulPlaybackHandler();

		final IPromise<PositionedPlaybackFile> positionedPlaybackHandlerContainer =
			new ExpectedPromise<>(() -> new PositionedPlaybackFile(0, playbackHandler, new File(1)));

		final IPromise<PositionedPlaybackFile> secondPositionedPlaybackHandlerContainer =
			new ExpectedPromise<>(() -> (this.expectedPositionedPlaybackFile = new PositionedPlaybackFile(0, playbackHandlerUnderTest, new File(1))));

		final IPreparedPlaybackFileQueue preparedPlaybackFileQueue = mock(IPreparedPlaybackFileQueue.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(secondPositionedPlaybackHandlerContainer);

		final PublishSubject<PositionedPlaybackFile> playbackFilePublisher = PublishSubject.create();
		new PlaylistPlayer(preparedPlaybackFileQueue, 0, playbackFilePublisher);

		playbackFilePublisher.subscribe(positionedPlaybackFile -> this.positionedPlaybackFile = positionedPlaybackFile);

		playbackHandler.resolve();
	}

	@Test
	public void thenTheChangeCanBeObserved() {
		assertThat(this.positionedPlaybackFile).isEqualTo(this.expectedPositionedPlaybackFile);
	}
}
